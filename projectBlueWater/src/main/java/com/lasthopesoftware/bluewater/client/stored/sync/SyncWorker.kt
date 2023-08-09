package com.lasthopesoftware.bluewater.client.stored.sync

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.access.DelegatingLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.stored.library.items.DelegatingStoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredFilesCounter
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesChecker
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesPruner
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.MediaItemCreator
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFilePathsLookup
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.client.stored.library.permissions.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.stored.library.permissions.write.StorageWritePermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncsHandler
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.SyncChannelProperties
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.SyncStartedReceiver
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileBroadcastReceiver
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileDownloadingNotifier
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileReadPermissionsReceiver
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileWritePermissionsReceiver
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.policies.caching.CachingPolicyFactory
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.FileStreamWriter
import com.lasthopesoftware.storage.FreeSpaceLookup
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

open class SyncWorker(private val context: Context, workerParams: WorkerParameters) :
	ListenableWorker(context, workerParams),
	PostSyncNotification
{
	companion object {
		private const val notificationId = 23
	}

	private val syncChecker by lazy {
		SyncChecker(
			LibraryRepository(context),
			serviceFilesCollector,
			StoredFilesChecker(StoredFilesCounter(StoredFilesCollection(context)))
		)
	}

	private val applicationMessageBus by lazy { getApplicationMessageBus().getScopedMessageBus() }
	private val storedFileAccess by lazy { StoredFileAccess(context) }
	private val readPermissionArbitratorForOs by lazy { OsPermissionsChecker(context) }
	private val libraryConnections by lazy { ConnectionSessionManager.get(context) }
	private val cachingPolicyFactory by lazy { CachingPolicyFactory() }

	private val libraryProvider by lazy { DelegatingLibraryProvider(LibraryRepository(context), cachingPolicyFactory) }

	private val fileProperties by lazy {
		val filePropertyCache = FilePropertyCache
		CachedFilePropertiesProvider(
			libraryConnections,
			filePropertyCache,
			FilePropertiesProvider(
				libraryConnections,
				LibraryRevisionProvider(libraryConnections),
				filePropertyCache
			)
		)
	}

	private val serviceFilesCollector by lazy {
		val serviceFilesCollector = StoredItemServiceFileCollector(
			StoredItemAccess(context),
			LibraryFileProvider(LibraryFileStringListProvider(libraryConnections)),
			FileListParameters
        )

		DelegatingStoredItemServiceFileCollector(serviceFilesCollector, cachingPolicyFactory)
	}

	private val storedFilesPruner by lazy {
		StoredFilesPruner(serviceFilesCollector, StoredFilesCollection(context), storedFileAccess)
	}

	private val storedFilesSynchronization by lazy {
		val contentResolver = context.contentResolver

		val cursorProvider = MediaQueryCursorProvider(contentResolver, storedFileAccess, fileProperties)

		val mediaFileUriProvider = MediaFileUriProvider(
			cursorProvider,
			readPermissionArbitratorForOs,
			true,
			applicationMessageBus
		)
		val storedFileUpdater = StoredFileUpdater(
            context,
			mediaFileUriProvider,
            MediaFileIdProvider(cursorProvider, readPermissionArbitratorForOs),
            StoredFileQuery(context),
            libraryProvider,
			StoredFilePathsLookup(
				fileProperties,
				SyncDirectoryLookup(
					libraryProvider,
					PublicDirectoryLookup(context),
					PrivateDirectoryLookup(context),
					FreeSpaceLookup
				)
			),
			MediaItemCreator(fileProperties, contentResolver)
        )

		val syncHandler = LibrarySyncsHandler(
			serviceFilesCollector,
			storedFilesPruner,
			storedFileUpdater,
			StoredFileJobProcessor(
				StoredFileUriDestinationBuilder(context.contentResolver),
				storedFileAccess,
				StoredFileDownloader(ServiceFileUriQueryParamsProvider, libraryConnections),
				FileReadPossibleArbitrator(),
				FileWritePossibleArbitrator,
				FileStreamWriter(context.contentResolver)
			)
		)

		StoredFileSynchronization(
			libraryProvider,
			applicationMessageBus,
			storedFilesPruner,
			syncChecker,
			syncHandler
		)
	}

	private val lazyStoredFileEventReceivers = lazy {
		val storedFileDownloadingNotifier = StoredFileDownloadingNotifier(
			storedFileAccess,
			fileProperties,
			this,
			context)

		val storedFileReadPermissionsReceiver = StoredFileReadPermissionsReceiver(
			readPermissionArbitratorForOs,
			StorageReadPermissionsRequestedBroadcaster(applicationMessageBus),
			storedFileAccess)
		val storedFileWritePermissionsReceiver = StoredFileWritePermissionsReceiver(
			OsPermissionsChecker(context),
			StorageWritePermissionsRequestedBroadcaster(applicationMessageBus),
			storedFileAccess)

		arrayOf(
			storedFileDownloadingNotifier,
			storedFileReadPermissionsReceiver,
			storedFileWritePermissionsReceiver
		)
	}

	private val syncStartedReceiver = lazy { SyncStartedReceiver(this) }

	private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val channelConfiguration by lazy { SyncChannelProperties(context) }

	private val activeNotificationChannelId by lazy {
		val notificationChannelActivator =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManager)
			else NoOpChannelActivator
		notificationChannelActivator.activateChannel(channelConfiguration)
	}

	private val intentBuilder by lazy { IntentBuilder(context) }

	private val showDownloadsIntent by lazy {
		intentBuilder.buildPendingShowDownloadsIntent()
	}

	private val cancelIntent by lazy { WorkManager.getInstance(context).createCancelPendingIntent(id) }

	private val notificationSync = Any()

	private val cancellationProxy = CancellationProxy()

	@Volatile
	private var activePromisedNotification = Unit.toPromise()

	final override fun startWork(): ListenableFuture<Result> {
		val futureResult = SettableFuture.create<Result>()

		doWork().then({ futureResult.set(Result.success()) }, futureResult::setException)

		return futureResult
	}

	final override fun onStopped() = cancellationProxy.run()

	private fun doWork(): Promise<Unit> {
		if (cancellationProxy.isCancelled) return Unit.toPromise()

		if (!lazyStoredFileEventReceivers.isInitialized()) {
			for (receiveStoredFileEvent in lazyStoredFileEventReceivers.value.distinct()) {
				val receiver = StoredFileBroadcastReceiver(receiveStoredFileEvent)
				for (acceptedMessage in receiveStoredFileEvent.acceptedEvents())
					applicationMessageBus.registerForClass(acceptedMessage, receiver)
			}
		}

		if (!syncStartedReceiver.isInitialized()) {
			applicationMessageBus.registerReceiver(syncStartedReceiver.value)
		}

		return if (cancellationProxy.isCancelled) {
			applicationMessageBus.close()
			Unit.toPromise()
		} else {
			storedFilesSynchronization.streamFileSynchronization()
				.toPromise()
				.also(cancellationProxy::doCancel)
				.inevitably {
					cancellationProxy.run() // Cancel any on-going processes
					synchronized(notificationSync) {
						activePromisedNotification
					}
				}
				.must {
					applicationMessageBus.close()
				}
		}
	}

	override fun notify(notificationText: String?) {
		val notifyBuilder = NotificationCompat.Builder(context, activeNotificationChannelId)
		notifyBuilder
			.setSmallIcon(R.drawable.ic_water_white)
			.setContentIntent(showDownloadsIntent)
			.addAction(0, context.getString(R.string.stop_sync_button), cancelIntent)
			.setOngoing(true)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
		notifyBuilder.setContentTitle(context.getText(R.string.title_sync_files))
		notifyBuilder.setContentText(notificationText)
		val syncNotification = notifyBuilder.build()
		val serviceType =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0

		synchronized(notificationSync) {
			if (cancellationProxy.isCancelled) return

			activePromisedNotification = activePromisedNotification.inevitably {
				if (cancellationProxy.isCancelled) Unit.toPromise()
				else setForegroundAsync(ForegroundInfo(notificationId, syncNotification, serviceType))
					.toPromise(ThreadPools.compute)
					.also(cancellationProxy::doCancel)
					.unitResponse()
			}
		}
	}
}
