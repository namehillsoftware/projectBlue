package com.lasthopesoftware.bluewater.client.stored.sync

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.broadcasts.ScanMediaFileBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.io.FileStreamWriter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.StorageWritePermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.stored.library.items.DelegatingStoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesPruner
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncsHandler
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.client.stored.sync.constraints.SyncWorkerConstraints
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.SyncChannelProperties
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.SyncStartedReceiver
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.*
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.makePendingIntentImmutable
import com.lasthopesoftware.bluewater.shared.policies.caching.CachingPolicyFactory
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.storage.FreeSpaceLookup
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SyncWorker(private val context: Context, workerParams: WorkerParameters) :
	ListenableWorker(context, workerParams),
	PostSyncNotification
{
	companion object {
		private const val notificationId = 23
		private const val workName = "StoredFilesSync"

		fun syncImmediately(context: Context): Promise<Operation> {
			val oneTimeWorkRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java)
			return WorkManager.getInstance(context)
				.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest.build())
				.result
				.toPromise(ThreadPools.compute)
				.eventually(
					{ scheduleSync(context) },
					{ scheduleSync(context) })
		}

		fun scheduleSync(context: Context): Promise<Operation> =
			constraints(context).then { c ->
				val periodicWorkRequest = PeriodicWorkRequest.Builder(SyncWorker::class.java, 3, TimeUnit.HOURS)
				periodicWorkRequest.setConstraints(c)
				WorkManager.getInstance(context)
					.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest.build())
			}

		fun cancelSync(context: Context): Promise<Unit> =
			promiseWorkInfos(context).then { wi ->
				val workManager = WorkManager.getInstance(context)
				for (item in wi.filter { it.state == WorkInfo.State.RUNNING })
					workManager.cancelWorkById(item.id)
			}

		private fun constraints(context: Context): Promise<Constraints> {
			val applicationSettings = context.getApplicationSettingsRepository()
			return SyncWorkerConstraints(applicationSettings).currentConstraints
		}

		fun promiseIsSyncing(context: Context): Promise<Boolean> =
			promiseWorkInfos(context).then { workInfos -> workInfos.any { wi -> wi.state == WorkInfo.State.RUNNING } }

		fun promiseIsScheduled(context: Context): Promise<Boolean> =
			promiseWorkInfos(context).then { workInfos -> workInfos.any { wi -> wi.state == WorkInfo.State.ENQUEUED } }

		private fun promiseWorkInfos(context: Context): Promise<List<WorkInfo>> =
			WorkManager.getInstance(context).getWorkInfosForUniqueWork(workName).toPromise(ThreadPools.compute)
	}

	private val syncChecker by lazy {
		SyncChecker(LibraryRepository(context), serviceFilesCollector)
	}

	private val applicationSettings by lazy { context.getApplicationSettingsRepository() }

	private val activeNotificationChannelId by lazy {
		val notificationChannelActivator =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManager)
			else NoOpChannelActivator()
		notificationChannelActivator.activateChannel(channelConfiguration)
	}

	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(context, BrowserEntryActivity::class.java)
		browseLibraryIntent.action = BrowserEntryActivity.showDownloadsAction
		browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
	}

	private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val channelConfiguration by lazy { SyncChannelProperties(context) }
	private val messageBus by lazy { MessageBus(LocalBroadcastManager.getInstance(context)) }
	private val storedFileAccess by lazy { StoredFileAccess(context) }
	private val readPermissionArbitratorForOs by lazy { ExternalStorageReadPermissionsArbitratorForOs(context) }
	private val libraryRepository by lazy { LibraryRepository(context) }
	private val libraryIdentifierProvider by lazy { SelectedBrowserLibraryIdentifierProvider(applicationSettings) }
	private val libraryConnections by lazy { ConnectionSessionManager.get(context) }

	private val fileProperties by lazy {
		val filePropertyCache = FilePropertyCache.getInstance()
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
			FileListParameters.getInstance())

		DelegatingStoredItemServiceFileCollector(
			serviceFilesCollector,
			CachingPolicyFactory()
		)
	}

	private val storedFilesPruner by lazy {
		StoredFilesPruner(
			serviceFilesCollector,
			StoredFilesCollection(context),
			storedFileAccess
		)
	}

	private val storedFilesSynchronization by lazy {
		val cursorProvider = MediaQueryCursorProvider(
			context,
			fileProperties
		)
		val storedFileUpdater = StoredFileUpdater(
			context,
			MediaFileUriProvider(
				context,
				cursorProvider,
				readPermissionArbitratorForOs,
				libraryIdentifierProvider,
				true
			),
			MediaFileIdProvider(
				cursorProvider,
				readPermissionArbitratorForOs
			),
			StoredFileQuery(context),
			libraryRepository,
			fileProperties,
			SyncDirectoryLookup(libraryRepository, PublicDirectoryLookup(context), PrivateDirectoryLookup(context), FreeSpaceLookup)
		)

		val syncHandler = LibrarySyncsHandler(
			serviceFilesCollector,
			storedFilesPruner,
			storedFileUpdater,
			StoredFileJobProcessor(
				StoredFileSystemFileProducer(),
				storedFileAccess,
				StoredFileDownloader(ServiceFileUriQueryParamsProvider(), libraryConnections),
				FileReadPossibleArbitrator(),
				FileWritePossibleArbitrator(),
				FileStreamWriter()
			)
		)
		StoredFileSynchronization(
			libraryRepository,
			messageBus,
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
		val storedFileMediaScannerNotifier =
			StoredFileMediaScannerNotifier(
				storedFileAccess,
				ScanMediaFileBroadcaster(context)
			)
		val storedFileReadPermissionsReceiver = StoredFileReadPermissionsReceiver(
			readPermissionArbitratorForOs,
			StorageReadPermissionsRequestedBroadcaster(messageBus),
			storedFileAccess)
		val storedFileWritePermissionsReceiver = StoredFileWritePermissionsReceiver(
			ExternalStorageWritePermissionsArbitratorForOs(context),
			StorageWritePermissionsRequestedBroadcaster(messageBus),
			storedFileAccess)

		arrayOf(
			storedFileDownloadingNotifier,
			storedFileMediaScannerNotifier,
			storedFileReadPermissionsReceiver,
			storedFileWritePermissionsReceiver)
	}

	private val syncStartedReceiver = lazy { SyncStartedReceiver(this) }

	private val showDownloadsIntent by lazy {
		PendingIntent.getActivity(
			context,
			0,
			browseLibraryIntent,
			0.makePendingIntentImmutable())
	}

	private val cancelIntent by lazy { WorkManager.getInstance(context).createCancelPendingIntent(id) }

	private val cancellationProxy = CancellationProxy()

	private val promisedNotifications = ConcurrentHashMap<Promise<Unit>, Unit>()

	override fun startWork(): ListenableFuture<Result> {
		val futureResult = SettableFuture.create<Result>()

		doWork().then({ futureResult.set(Result.success()) }, futureResult::setException)

		return futureResult
	}

	override fun onStopped() = cancellationProxy.run()

	override fun notify(notificationText: String?) {
		val notifyBuilder = NotificationCompat.Builder(context, activeNotificationChannelId)
		notifyBuilder
			.setSmallIcon(R.drawable.ic_stat_water_drop_white)
			.setContentIntent(showDownloadsIntent)
			.addAction(0, context.getString(R.string.stop_sync_button), cancelIntent)
			.setOngoing(true)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
		notifyBuilder.setContentTitle(context.getText(R.string.title_sync_files))
		notifyBuilder.setContentText(notificationText)
		val syncNotification = notifyBuilder.build()
		val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
		val promisedForegroundNotification = setForegroundAsync(ForegroundInfo(notificationId, syncNotification, serviceType))
				.toPromise(ThreadPools.compute)
				.unitResponse()
		promisedNotifications.putIfAbsent(promisedForegroundNotification, Unit)
		promisedForegroundNotification.must { promisedNotifications.remove(promisedForegroundNotification) }
	}

	private fun doWork(): Promise<Unit> {
		if (cancellationProxy.isCancelled) return Unit.toPromise()

		if (!lazyStoredFileEventReceivers.isInitialized()) {
			for (receiveStoredFileEvent in lazyStoredFileEventReceivers.value.distinct()) {
				val broadcastReceiver = StoredFileBroadcastReceiver(receiveStoredFileEvent)
				messageBus.registerReceiver(
					broadcastReceiver,
					receiveStoredFileEvent.acceptedEvents().fold(IntentFilter(), { i, e ->
						i.addAction(e)
						i
					}))
			}
		}

		if (!syncStartedReceiver.isInitialized()) {
			messageBus.registerReceiver(
				syncStartedReceiver.value,
				syncStartedReceiver.value.acceptedEvents().fold(IntentFilter(), { i, e ->
					i.addAction(e)
					i
				}))
		}

		return if (cancellationProxy.isCancelled) Unit.toPromise()
		else storedFilesSynchronization.streamFileSynchronization()
			.toPromise()
			.also(cancellationProxy::doCancel)
			.inevitably { Promise.whenAll(promisedNotifications.keys) }
			.must { messageBus.clear() }
	}
}
