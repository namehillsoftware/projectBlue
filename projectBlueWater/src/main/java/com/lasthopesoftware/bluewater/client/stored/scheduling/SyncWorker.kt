package com.lasthopesoftware.bluewater.client.stored.scheduling

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
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
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer
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
import com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.SyncWorkerConstraints
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService
import com.lasthopesoftware.bluewater.client.stored.service.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.client.stored.service.notifications.SyncChannelProperties
import com.lasthopesoftware.bluewater.client.stored.service.receivers.SyncStartedReceiver
import com.lasthopesoftware.bluewater.client.stored.service.receivers.file.*
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.makePendingIntentImmutable
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
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SyncWorker(private val context: Context, workerParams: WorkerParameters) :
	ListenableWorker(context, workerParams),
	PostSyncNotification
{


	companion object {
		private val logger by lazy { LoggerFactory.getLogger(SyncWorker::class.java) }
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(SyncWorker::class.java) }
		private const val notificationId = 23
		private val workName by lazy { magicPropertyBuilder.buildProperty("") }

		@JvmStatic
		fun scheduleSync(context: Context): Promise<Operation> {
			return constraints(context).then { c ->
				val periodicWorkRequest = PeriodicWorkRequest.Builder(SyncWorker::class.java, 3, TimeUnit.HOURS)
				periodicWorkRequest.setConstraints(c)
				WorkManager.getInstance(context)
					.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest.build())
			}
		}

		private fun constraints(context: Context): Promise<Constraints> {
			val applicationSettings = context.getApplicationSettingsRepository()
			return SyncWorkerConstraints(applicationSettings).currentConstraints
		}

		fun promiseIsScheduled(context: Context): Promise<Boolean> {
			return promiseWorkInfos(context)
				.then { workInfos -> workInfos.any { wi -> wi.state == WorkInfo.State.ENQUEUED } }
		}

		private fun promiseWorkInfos(context: Context): Promise<List<WorkInfo>> {
			return WorkManager.getInstance(context).getWorkInfosForUniqueWork(workName).toPromise(ThreadPools.compute)
		}
	}

	private var isSyncRunning = false

	private val lazySyncChecker by lazy {
			SyncChecker(
				LibraryRepository(context),
				StoredItemServiceFileCollector(
					StoredItemAccess(context),
					LibraryFileProvider(LibraryFileStringListProvider(ConnectionSessionManager.get(context))),
					FileListParameters.getInstance()))
	}

	private val applicationSettings by lazy { context.getApplicationSettingsRepository() }

	private val lazyActiveNotificationChannelId by lazy {
		val notificationChannelActivator =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManagerLazy)
			else NoOpChannelActivator()
		notificationChannelActivator.activateChannel(lazyChannelConfiguration)
	}

	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(context, BrowserEntryActivity::class.java)
		browseLibraryIntent.action = BrowserEntryActivity.showDownloadsAction
		browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
		browseLibraryIntent
	}

	private val notificationManagerLazy by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val lazyChannelConfiguration by lazy { SyncChannelProperties(context) }
	private val lazyMessageBus = lazy { MessageBus(LocalBroadcastManager.getInstance(context)) }
	private val lazyStoredFileAccess by lazy { StoredFileAccess(context, StoredFilesCollection(context)) }
	private val lazyReadPermissionArbitratorForOs by lazy { ExternalStorageReadPermissionsArbitratorForOs(context) }
	private val lazyLibraryRepository by lazy { LibraryRepository(context) }
	private val lazyLibraryIdentifierProvider by lazy { SelectedBrowserLibraryIdentifierProvider(applicationSettings) }
	private val lazyLibraryConnections by lazy { ConnectionSessionManager.get(context) }

	private val lazyFileProperties by lazy {
		val filePropertyCache = FilePropertyCache.getInstance()
		CachedFilePropertiesProvider(
			lazyLibraryConnections,
			filePropertyCache,
			FilePropertiesProvider(
				lazyLibraryConnections,
				LibraryRevisionProvider(lazyLibraryConnections),
				filePropertyCache)
		)
	}

	private val lazyStoredFilesSynchronization by lazy {
		val storedItemAccess = StoredItemAccess(context)
		val cursorProvider = MediaQueryCursorProvider(
			context,
			lazyFileProperties)
		val storedFileUpdater = StoredFileUpdater(
			context,
			MediaFileUriProvider(
				context,
				cursorProvider,
				lazyReadPermissionArbitratorForOs,
				lazyLibraryIdentifierProvider,
				true),
			MediaFileIdProvider(
				cursorProvider,
				lazyReadPermissionArbitratorForOs),
			StoredFileQuery(context),
			lazyLibraryRepository,
			lazyFileProperties,
			SyncDirectoryLookup(lazyLibraryRepository, PublicDirectoryLookup(context), PrivateDirectoryLookup(context), FreeSpaceLookup)
		)
		val syncHandler = LibrarySyncsHandler(
			StoredItemServiceFileCollector(
				storedItemAccess,
				LibraryFileProvider(LibraryFileStringListProvider(lazyLibraryConnections)),
				FileListParameters.getInstance()),
			lazyStoredFileAccess,
			storedFileUpdater,
			StoredFileJobProcessor(
				StoredFileSystemFileProducer(),
				lazyStoredFileAccess,
				StoredFileDownloader(ServiceFileUriQueryParamsProvider(), lazyLibraryConnections),
				FileReadPossibleArbitrator(),
				FileWritePossibleArbitrator(),
				FileStreamWriter()
			)
		)
		StoredFileSynchronization(
			lazyLibraryRepository,
			lazyMessageBus.value,
			syncHandler)
	}

	private val lazyStoredFileEventReceivers = lazy {
		val storedFileDownloadingNotifier = StoredFileDownloadingNotifier(
			lazyStoredFileAccess,
			lazyFileProperties,
			this,
			context)
		val storedFileMediaScannerNotifier = StoredFileMediaScannerNotifier(
			lazyStoredFileAccess,
			ScanMediaFileBroadcaster(context)
		)
		val storedFileReadPermissionsReceiver = StoredFileReadPermissionsReceiver(
			lazyReadPermissionArbitratorForOs,
			StorageReadPermissionsRequestedBroadcaster(lazyMessageBus.value),
			lazyStoredFileAccess)
		val storedFileWritePermissionsReceiver = StoredFileWritePermissionsReceiver(
			ExternalStorageWritePermissionsArbitratorForOs(context),
			StorageWritePermissionsRequestedBroadcaster(lazyMessageBus.value),
			lazyStoredFileAccess)

		arrayOf(
			storedFileDownloadingNotifier,
			storedFileMediaScannerNotifier,
			storedFileReadPermissionsReceiver,
			storedFileWritePermissionsReceiver)
	}

	private val lazySyncStartedReceiver = lazy { SyncStartedReceiver(this) }

	private val wakeLock = lazy {
		val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
		powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "wakeLock"))
	}

	private val lazyShowDownloadsIntent by lazy {
		PendingIntent.getActivity(
			context,
			0,
			browseLibraryIntent,
			0.makePendingIntentImmutable())
	}

	private val lazyCancelIntent by lazy {
		WorkManager.getInstance(context).createCancelPendingIntent(id)
	}

	private val broadcastReceivers: MutableList<BroadcastReceiver> = ArrayList()

	private val promisedNotifications = ConcurrentHashMap<Promise<Unit>, Unit>()

	private var promisedSynchronization: Promise<Unit>? = null

	override fun startWork(): ListenableFuture<Result> {
		val futureResult = SettableFuture.create<Result>()
		lazySyncChecker.promiseIsSyncNeeded()
			.eventually { isNeeded ->
				if (isNeeded) doWork()
				else Unit.toPromise()
			}
			.then({ futureResult.set(Result.success()) }, { futureResult.setException(it) })
		return futureResult
	}

	private fun doWork(): Promise<Unit> {
		val isUninterruptedSync = false

		if (isSyncRunning) {
			if (isUninterruptedSync) {
				if (onWifiStateChangedReceiver.isInitialized()) context.unregisterReceiver(onWifiStateChangedReceiver.value)
				if (onPowerDisconnectedReceiver.isInitialized()) context.unregisterReceiver(onPowerDisconnectedReceiver.value)
			}
			logger.info("Sync already running, not starting again")
			return Unit.toPromise()
		}

		if (!lazyStoredFileEventReceivers.isInitialized()) {
			for (receiveStoredFileEvent in lazyStoredFileEventReceivers.value) {
				val broadcastReceiver = StoredFileBroadcastReceiver(receiveStoredFileEvent)
				if (!broadcastReceivers.add(broadcastReceiver)) continue
				lazyMessageBus.value.registerReceiver(
					broadcastReceiver,
					receiveStoredFileEvent.acceptedEvents().fold(IntentFilter(), { i, e ->
						i.addAction(e)
						i
					}))
			}
		}

		if (!lazySyncStartedReceiver.isInitialized()) {
			lazyMessageBus.value.registerReceiver(
				lazySyncStartedReceiver.value,
				lazySyncStartedReceiver.value.acceptedEvents().fold(IntentFilter(), { i, e ->
					i.addAction(e)
					i
				}))
		}

		if (!isUninterruptedSync) {
			context.registerReceiver(onWifiStateChangedReceiver.value, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
			context.registerReceiver(onPowerDisconnectedReceiver.value, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
		}

		isSyncRunning = true
		return Promise.whenAll(
			promisedNotifications.keys
				.plus(lazyStoredFilesSynchronization.streamFileSynchronization()
				.toPromise().also { promisedSynchronization = it }))
			.unitResponse()
	}

	override fun onStopped() {
		promisedSynchronization?.cancel()
	}

	override fun notify(notificationText: String?) {
		val notifyBuilder = NotificationCompat.Builder(context, lazyActiveNotificationChannelId)
		notifyBuilder
			.setSmallIcon(R.drawable.ic_stat_water_drop_white)
			.setContentIntent(lazyShowDownloadsIntent)
			.addAction(0, context.getString(R.string.stop_sync_button), lazyCancelIntent)
			.setOngoing(true)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
		notifyBuilder.setContentTitle(context.getText(R.string.title_sync_files))
		notifyBuilder.setContentText(notificationText)
		val syncNotification = notifyBuilder.build()
		val promisedForegroundNotification = setForegroundAsync(ForegroundInfo(notificationId, syncNotification))
			.toPromise(ThreadPools.compute)
			.unitResponse()
		promisedNotifications.putIfAbsent(promisedForegroundNotification, Unit)
		promisedForegroundNotification.must { promisedNotifications.remove(promisedForegroundNotification) }
	}
}
