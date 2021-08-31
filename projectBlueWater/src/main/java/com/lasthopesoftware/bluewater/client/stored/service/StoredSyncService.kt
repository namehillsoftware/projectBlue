package com.lasthopesoftware.bluewater.client.stored.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
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
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.client.stored.service.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.client.stored.service.notifications.SyncChannelProperties
import com.lasthopesoftware.bluewater.client.stored.service.receivers.SyncStartedReceiver
import com.lasthopesoftware.bluewater.client.stored.service.receivers.file.*
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.bluewater.settings.repository.access.ApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.GenericBinder
import com.lasthopesoftware.bluewater.shared.IoCommon
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.storage.FreeSpaceLookup
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.util.*

class StoredSyncService : Service(), PostSyncNotification {

	companion object {
		private val logger = LoggerFactory.getLogger(StoredSyncService::class.java)
		private val doSyncAction = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "doSyncAction")
		private val isUninterruptedSyncSetting = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "isUninterruptedSyncSetting")
		private val cancelSyncAction = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "cancelSyncAction")
		private val lastSyncTime = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "lastSyncTime")
		private const val notificationId = 23

		@JvmStatic
		fun doSync(context: Context) {
			val intent = Intent(context, StoredSyncService::class.java)
			intent.action = doSyncAction
			safelyStartService(context, intent)
		}

		@JvmStatic
		fun doSyncUninterruptedFromUiThread(context: Context) {
			val intent = Intent(context, StoredSyncService::class.java)
			intent.action = doSyncAction
			intent.putExtra(isUninterruptedSyncSetting, true)
			context.startService(intent)
		}

		fun cancelSync(context: Context) {
			context.startService(getSelfIntent(context, cancelSyncAction))
		}

		var isSyncRunning: Boolean = false
			private set

		private fun safelyStartService(context: Context, intent: Intent) {
			try {
				ContextCompat.startForegroundService(context, intent)
			} catch (e: IllegalStateException) {
				logger.warn("An illegal state exception occurred while trying to start the service", e)
			} catch (e: SecurityException) {
				logger.warn("A security exception occurred while trying to start the service", e)
			}
		}

		private fun getSelfIntent(context: Context, action: String): Intent {
			val intent = Intent(context, StoredSyncService::class.java)
			intent.action = action
			return intent
		}
	}

	private val lazySharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

	private val applicationSettings by lazy { ApplicationSettingsRepository(this) }

	private val onWifiStateChangedReceiver = lazy {
		object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent) {
				applicationSettings.promiseApplicationSettings()
					.eventually(LoopedInPromise.response({ s ->
						if (s.isSyncOnWifiOnly && !IoCommon.isWifiConnected(context)) cancelSync(this@StoredSyncService)
					}, context))
			}
		}
	}

	private val onPowerDisconnectedReceiver = lazy {
		object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent) {
				applicationSettings.promiseApplicationSettings()
					.then { a -> if (a.isSyncOnPowerOnly) cancelSync(context) }
			}
		}
	}

	private val lazyActiveNotificationChannelId by lazy {
		val notificationChannelActivator =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManagerLazy)
			else NoOpChannelActivator()
		notificationChannelActivator.activateChannel(lazyChannelConfiguration)
	}

	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(this, BrowserEntryActivity::class.java)
		browseLibraryIntent.action = BrowserEntryActivity.showDownloadsAction
		browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
		browseLibraryIntent
	}

	private val notificationManagerLazy by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val lazyChannelConfiguration by lazy { SyncChannelProperties(this) }
	private val lazyMessageBus = lazy { MessageBus(LocalBroadcastManager.getInstance(this)) }
	private val lazyStoredFileAccess by lazy { StoredFileAccess(this, StoredFilesCollection(this)) }
	private val lazyReadPermissionArbitratorForOs by lazy { ExternalStorageReadPermissionsArbitratorForOs(this) }
	private val lazyLibraryRepository by lazy { LibraryRepository(this) }
	private val lazyLibraryIdentifierProvider by lazy { SelectedBrowserLibraryIdentifierProvider(applicationSettings) }
	private val lazyLibraryConnections by lazy { ConnectionSessionManager.get(this)	}

	private val lazyFileProperties by lazy {
		val filePropertyCache = FilePropertyCache.getInstance()
		CachedFilePropertiesProvider(
			lazyLibraryConnections,
			filePropertyCache,
			FilePropertiesProvider(
				lazyLibraryConnections,
				LibraryRevisionProvider(lazyLibraryConnections),
				filePropertyCache))
	}

	private val lazyStoredFilesSynchronization by lazy {
		val storedSyncService = this
		val storedItemAccess = StoredItemAccess(storedSyncService)
		val cursorProvider = MediaQueryCursorProvider(
			storedSyncService,
			lazyFileProperties)
		val storedFileUpdater = StoredFileUpdater(
			storedSyncService,
			MediaFileUriProvider(
				storedSyncService,
				cursorProvider,
				lazyReadPermissionArbitratorForOs,
				lazyLibraryIdentifierProvider,
				true),
			MediaFileIdProvider(
				cursorProvider,
				lazyReadPermissionArbitratorForOs),
			StoredFileQuery(storedSyncService),
			lazyLibraryRepository,
			lazyFileProperties,
			SyncDirectoryLookup(lazyLibraryRepository, PublicDirectoryLookup(storedSyncService), PrivateDirectoryLookup(storedSyncService), FreeSpaceLookup))
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
				FileStreamWriter()))
		StoredFileSynchronization(
			lazyLibraryRepository,
			lazyMessageBus.value,
			syncHandler)
	}

	private val lazyStoredFileEventReceivers = lazy {
		val storedSyncService = this
		val storedFileDownloadingNotifier = StoredFileDownloadingNotifier(
			lazyStoredFileAccess,
			lazyFileProperties,
			storedSyncService,
			storedSyncService)
		val storedFileMediaScannerNotifier = StoredFileMediaScannerNotifier(
			lazyStoredFileAccess,
			ScanMediaFileBroadcaster(this))
		val storedFileReadPermissionsReceiver = StoredFileReadPermissionsReceiver(
			lazyReadPermissionArbitratorForOs,
			StorageReadPermissionsRequestedBroadcaster(lazyMessageBus.value),
			lazyStoredFileAccess)
		val storedFileWritePermissionsReceiver = StoredFileWritePermissionsReceiver(
			ExternalStorageWritePermissionsArbitratorForOs(this),
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
		val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
		powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "wakeLock"))
	}

	private val lazyShowDownloadsIntent by lazy {
		PendingIntent.getActivity(
			this,
			0,
			browseLibraryIntent,
			0)
	}

	private val lazyCancelIntent by lazy {
		PendingIntent.getService(
			this,
			0,
			getSelfIntent(this, cancelSyncAction),
			PendingIntent.FLAG_UPDATE_CURRENT)
	}

	private val broadcastReceivers: MutableList<BroadcastReceiver> = ArrayList()

	private var synchronizationDisposable: Disposable? = null

	override fun onCreate() {
		super.onCreate()
		wakeLock.value.acquire()
	}

	@Synchronized
	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

		val action = intent.action

		if (cancelSyncAction == action) {
			finish()
			return START_NOT_STICKY
		}

		if (doSyncAction != action) {
			logger.info("$action was not $doSyncAction, not starting sync")
			return START_NOT_STICKY
		}

		val isUninterruptedSync = intent.getBooleanExtra(isUninterruptedSyncSetting, false)

		if (isSyncRunning) {
			if (isUninterruptedSync) {
				if (onWifiStateChangedReceiver.isInitialized()) unregisterReceiver(onWifiStateChangedReceiver.value)
				if (onPowerDisconnectedReceiver.isInitialized()) unregisterReceiver(onPowerDisconnectedReceiver.value)
			}
			logger.info("Sync already running, not starting again")
			return START_NOT_STICKY
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
			registerReceiver(onWifiStateChangedReceiver.value, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
			registerReceiver(onPowerDisconnectedReceiver.value, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
		}

		isSyncRunning = true
		synchronizationDisposable = lazyStoredFilesSynchronization
			.streamFileSynchronization()
			.subscribe(::finish, ::finish)

		return START_NOT_STICKY
	}

	private fun finish(error: Throwable) {
		logger.error("An error occurred while synchronizing stored files", error)
		finish()
	}

	private fun finish() {
		lazySharedPreferences
			.edit()
			.putLong(lastSyncTime, DateTime.now().millis)
			.apply()
		stopSelf()
	}

	override fun onDestroy() {
		synchronizationDisposable?.dispose()
		isSyncRunning = false

		if (lazyMessageBus.isInitialized()) {
			while (broadcastReceivers.isNotEmpty()) {
				val receiver = broadcastReceivers.removeAt(0)
				lazyMessageBus.value.unregisterReceiver(receiver)
			}
		}

		if (onWifiStateChangedReceiver.isInitialized()) unregisterReceiver(onWifiStateChangedReceiver.value)
		if (onPowerDisconnectedReceiver.isInitialized()) unregisterReceiver(onPowerDisconnectedReceiver.value)
		if (wakeLock.isInitialized()) wakeLock.value.release()

		stopForeground(true)

		super.onDestroy()
	}

	override fun onBind(intent: Intent): IBinder = lazyBinder

	private val lazyBinder by lazy { GenericBinder(this) }

	override fun notify(notificationText: String?) {
		val notifyBuilder = NotificationCompat.Builder(this, lazyActiveNotificationChannelId)
		notifyBuilder
			.setSmallIcon(R.drawable.ic_stat_water_drop_white)
			.setContentIntent(lazyShowDownloadsIntent)
			.addAction(0, getString(R.string.stop_sync_button), lazyCancelIntent)
			.setOngoing(true)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
		notifyBuilder.setContentTitle(getText(R.string.title_sync_files))
		notifyBuilder.setContentText(notificationText)
		val syncNotification = notifyBuilder.build()
		startForeground(notificationId, syncNotification)
	}
}
