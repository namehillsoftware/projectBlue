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
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.DelegatingLibraryProvider
import com.lasthopesoftware.bluewater.client.connection.PacketSender
import com.lasthopesoftware.bluewater.client.connection.libraries.DelegatingLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionRegistry
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.CachedDataSourceServerConnectionProvider
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryConnectionSettings
import com.lasthopesoftware.bluewater.client.stored.library.items.DelegatingStoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredFilesCounter
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesChecker
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesPruner
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.CompatibleMediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.ExternalContentRepository
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUrisLookup
import com.lasthopesoftware.bluewater.client.stored.library.permissions.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncsHandler
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.SyncChannelProperties
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.SyncStartedReceiver
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileBroadcastReceiver
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileDownloadingNotifier
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileReadPermissionsReceiver
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.policies.caching.PermanentCachePolicy
import com.lasthopesoftware.promises.extensions.toListenableFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.OsFileSupplier
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import com.lasthopesoftware.storage.FreeSpaceLookup
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleTester
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

open class SyncWorker(private val context: Context, workerParams: WorkerParameters) :
	ListenableWorker(context, workerParams),
	PostSyncNotification
{
	companion object {
		private const val notificationId = 23
	}

	private val applicationDependencies by lazy { context.applicationDependencies }

	private val cachingPolicyFactory by lazy { PermanentCachePolicy }

	private val libraryConnections by lazy {
		with (applicationDependencies) {
			val connectionSettingsLookup = SyncLibraryConnectionSettings(librarySettingsProvider)

			val serverLookup = ServerLookup(
				connectionSettingsLookup,
				ServerInfoXmlRequest(connectionSettingsLookup, okHttpClients),
            )

			val activeNetwork = ActiveNetworkFinder(context)
			DelegatingLibraryConnectionProvider(
				LibraryConnectionProvider(
					connectionSettingsLookup,
					ServerAlarm(serverLookup, activeNetwork, ServerWakeSignal(PacketSender())),
					CachedDataSourceServerConnectionProvider(
						LiveServerConnectionProvider(
							activeNetwork,
							Base64Encoder,
							serverLookup,
							connectionSettingsLookup,
							okHttpClients,
							okHttpClients,
							JsonEncoderDecoder,
							stringResources,
						),
						audioCacheStreamSupplier,
					),
					AlarmConfiguration.standard
				),
				cachingPolicyFactory
			)
		}
	}

	private val libraryConnectionDependents by lazy { LibraryConnectionRegistry(applicationDependencies) }
	private val applicationMessageBus by lazy { getApplicationMessageBus().getScopedMessageBus() }
	private val storedFileAccess by lazy { StoredFileAccess(context) }
	private val readPermissionArbitratorForOs by lazy { OsPermissionsChecker(context) }

	private val serviceFilesCollector by lazy {
		val serviceFilesCollector = StoredItemServiceFileCollector(
			applicationDependencies.storedItemAccess,
			libraryConnectionDependents.libraryFilesProvider
		)

		DelegatingStoredItemServiceFileCollector(serviceFilesCollector, cachingPolicyFactory)
	}

	private val libraryProvider by lazy {
		DelegatingLibraryProvider(
			applicationDependencies.libraryProvider,
			cachingPolicyFactory
		)
	}

	private val syncChecker by lazy {
		SyncChecker(
			libraryProvider,
			serviceFilesCollector,
			StoredFilesChecker(StoredFilesCounter(storedFileAccess))
		)
	}

	private val fileProperties by lazy { libraryConnectionDependents.libraryFilePropertiesProvider }

	private val externalContentRepository by lazy {
		ExternalContentRepository(
            context.contentResolver,
			PublicDirectoryLookup(context),
		)
	}

	private val storedFilesPruner by lazy {
		StoredFilesPruner(serviceFilesCollector, storedFileAccess, externalContentRepository)
	}

	private val storedFilesSynchronization by lazy {
		val contentResolver = context.contentResolver

		val mediaFileUriProvider = CompatibleMediaFileUriProvider(
			fileProperties,
			readPermissionArbitratorForOs,
			contentResolver,
		)

		val storedFileUpdater = StoredFileUpdater(
            storedFileAccess,
			mediaFileUriProvider,
			libraryProvider,
			StoredFileUrisLookup(
				fileProperties,
				applicationDependencies.librarySettingsProvider,
				SyncDirectoryLookup(
					applicationDependencies.librarySettingsProvider,
                    PrivateDirectoryLookup(context),
					FreeSpaceLookup
				),
				mediaFileUriProvider,
				externalContentRepository
			),
			externalContentRepository
		)

		val syncHandler = LibrarySyncsHandler(
			serviceFilesCollector,
			storedFilesPruner,
			storedFileUpdater,
			StoredFileJobProcessor(
				StoredFileUriDestinationBuilder(OsFileSupplier, FileWritePossibleTester, context.contentResolver),
				StoredFileDownloader(libraryConnections),
				storedFileUpdater,
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

		arrayOf(
			storedFileDownloadingNotifier,
			storedFileReadPermissionsReceiver,
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

	private val showDownloadsIntent by lazy { context.applicationDependencies.intentBuilder.buildPendingShowDownloadsIntent() }

	private val cancelIntent by lazy { WorkManager.getInstance(context).createCancelPendingIntent(id) }

	private val notificationSync = Any()

	private val cancellationProxy = CancellationProxy()

	@Volatile
	private var activePromisedNotification = Unit.toPromise()

	final override fun startWork(): ListenableFuture<Result> =
		doWork().then { _ -> Result.success() }.toListenableFuture()

	final override fun onStopped() = cancellationProxy.cancellationRequested()

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
					cancellationProxy.cancellationRequested() // Cancel any on-going processes
					synchronized(notificationSync) {
						activePromisedNotification
					}
				}
				.must { _ ->
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
