package com.lasthopesoftware.bluewater.client.playback.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.media.RemoteControlClient
import android.os.*
import android.os.PowerManager.WakeLock
import android.support.v4.media.session.MediaSessionCompat
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.audio.AudioCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.CachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.*
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache.MemoryCachedImageAccess
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.*
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider.Instance.get
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.pollSessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine.Companion.createEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueFeederBuilder
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.HttpDataSourceFactoryProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.MediaSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.QueueProviders
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparationProvider
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Action.Bag
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.*
import com.lasthopesoftware.bluewater.client.playback.service.exceptions.ConnectionCircuitTracker
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.MediaStyleNotificationSetup
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.NowPlayingNotificationBuilder
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.PlaybackStartingNotificationBuilder
import com.lasthopesoftware.bluewater.client.playback.service.receivers.AudioBecomingNoisyReceiver
import com.lasthopesoftware.bluewater.client.playback.service.receivers.MediaSessionCallbackReceiver
import com.lasthopesoftware.bluewater.client.playback.service.receivers.RemoteControlReceiver
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.RemoteControlProxy
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected.RemoteControlClientBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.NowPlayingActivity.Companion.startNowPlayingActivity
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.bluewater.settings.volumeleveling.VolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.GenericBinder
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder.Companion.buildMagicPropertyName
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.resources.loopers.HandlerThreadCreator
import com.lasthopesoftware.resources.notifications.NoOpChannelActivator
import com.lasthopesoftware.resources.notifications.NotificationBuilderProducer
import com.lasthopesoftware.resources.notifications.control.NotificationsController
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.VoidResponse
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.RxThreadFactory
import io.reactivex.internal.schedulers.SingleScheduler
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

open class PlaybackService : Service(), OnAudioFocusChangeListener {

	companion object {
		private val logger = LoggerFactory.getLogger(PlaybackService::class.java)
		private val mediaSessionTag = buildMagicPropertyName(PlaybackService::class.java, "mediaSessionTag")
		private const val playingNotificationId = 42
		private const val startingNotificationId = 53
		private const val connectingNotificationId = 70

		private val lazyObservationScheduler = lazy {
			SingleScheduler(
					RxThreadFactory(
						"Playback Observation",
						Thread.MIN_PRIORITY,
						false
					))
		}

		private fun getNewSelfIntent(context: Context, action: String): Intent {
			val newIntent = Intent(context, PlaybackService::class.java)
			newIntent.action = action
			return newIntent
		}

		@JvmStatic
		fun launchMusicService(context: Context, serializedFileList: String?) {
			launchMusicService(context, 0, serializedFileList)
		}

		@JvmStatic
		fun launchMusicService(context: Context, filePos: Int, serializedFileList: String?) {
			val svcIntent = getNewSelfIntent(context, Action.launchMusicService)
			svcIntent.putExtra(Bag.playlistPosition, filePos)
			svcIntent.putExtra(Bag.filePlaylist, serializedFileList)
			safelyStartService(context, svcIntent)
		}

		@JvmOverloads
		@JvmStatic
		fun seekTo(context: Context, filePos: Int, fileProgress: Int = 0) {
			val svcIntent = getNewSelfIntent(context, Action.seekTo)
			svcIntent.putExtra(Bag.playlistPosition, filePos)
			svcIntent.putExtra(Bag.startPos, fileProgress)
			safelyStartService(context, svcIntent)
		}

		@JvmStatic
		fun play(context: Context) {
			safelyStartService(context, getNewSelfIntent(context, Action.play))
		}

		@JvmStatic
		fun pendingPlayingIntent(context: Context): PendingIntent {
			return PendingIntent.getService(
				context,
				0,
				getNewSelfIntent(
					context,
					Action.play),
				PendingIntent.FLAG_UPDATE_CURRENT)
		}

		@JvmStatic
		fun pause(context: Context) {
			safelyStartService(context, getNewSelfIntent(context, Action.pause))
		}

		@JvmStatic
		fun pendingPauseIntent(context: Context): PendingIntent {
			return PendingIntent.getService(
				context,
				0,
				getNewSelfIntent(
					context,
					Action.pause),
				PendingIntent.FLAG_UPDATE_CURRENT)
		}

		@JvmStatic
		fun togglePlayPause(context: Context) {
			safelyStartService(context, getNewSelfIntent(context, Action.togglePlayPause))
		}

		@JvmStatic
		fun next(context: Context) {
			safelyStartService(context, getNewSelfIntent(context, Action.next))
		}

		@JvmStatic
		fun pendingNextIntent(context: Context): PendingIntent {
			return PendingIntent.getService(
				context,
				0,
				getNewSelfIntent(
					context,
					Action.next),
				PendingIntent.FLAG_UPDATE_CURRENT)
		}

		@JvmStatic
		fun previous(context: Context) {
			safelyStartService(context, getNewSelfIntent(context, Action.previous))
		}

		@JvmStatic
		fun pendingPreviousIntent(context: Context): PendingIntent {
			return PendingIntent.getService(
				context,
				0,
				getNewSelfIntent(
					context,
					Action.previous),
				PendingIntent.FLAG_UPDATE_CURRENT)
		}

		@JvmStatic
		fun setRepeating(context: Context) {
			safelyStartService(context, getNewSelfIntent(context, Action.repeating))
		}

		@JvmStatic
		fun setCompleting(context: Context) {
			safelyStartService(context, getNewSelfIntent(context, Action.completing))
		}

		@JvmStatic
		fun addFileToPlaylist(context: Context, fileKey: Int) {
			val intent = getNewSelfIntent(context, Action.addFileToPlaylist)
			intent.putExtra(Bag.playlistPosition, fileKey)
			safelyStartService(context, intent)
		}

		@JvmStatic
		fun removeFileAtPositionFromPlaylist(context: Context, filePosition: Int) {
			val intent = getNewSelfIntent(context, Action.removeFileAtPositionFromPlaylist)
			intent.putExtra(Bag.filePosition, filePosition)
			safelyStartService(context, intent)
		}

		@JvmStatic
		fun killService(context: Context) {
			safelyStartService(context, getNewSelfIntent(context, Action.killMusicService))
		}

		@JvmStatic
		fun pendingKillService(context: Context): PendingIntent {
			return PendingIntent.getService(
				context,
				0,
				getNewSelfIntent(
					context,
					Action.killMusicService),
				PendingIntent.FLAG_UPDATE_CURRENT)
		}

		private fun safelyStartService(context: Context, intent: Intent) {
			try {
				context.startService(intent)
			} catch (e: IllegalStateException) {
				logger.warn("An illegal state exception occurred while trying to start the service", e)
			} catch (e: SecurityException) {
				logger.warn("A security exception occurred while trying to start the service", e)
			}
		}

		private fun buildFullNotification(notificationBuilder: NotificationCompat.Builder): Notification {
			return notificationBuilder
				.setSmallIcon(R.drawable.clearstream_logo_dark)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.build()
		}

		private fun buildRemoteControlProxyIntentFilter(remoteControlProxy: RemoteControlProxy): IntentFilter {
			val intentFilter = IntentFilter()
			for (action in remoteControlProxy.registerForIntents()) {
				intentFilter.addAction(action)
			}
			return intentFilter
		}

		private fun buildNotificationRouterIntentFilter(playbackNotificationRouter: PlaybackNotificationRouter): IntentFilter {
			val intentFilter = IntentFilter()
			for (action in playbackNotificationRouter.registerForIntents()) {
				intentFilter.addAction(action)
			}
			return intentFilter
		}
	}

	/* End streamer intent helpers */

	var isPlaying = false
		private set

	private val lazyBinder = lazy { GenericBinder(this) }
	private val notificationManagerLazy = lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
	private val audioManagerLazy = lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
	private val localBroadcastManagerLazy = lazy { LocalBroadcastManager.getInstance(this) }
	private val remoteControlReceiver = lazy { ComponentName(packageName, RemoteControlReceiver::class.java.name) }
	private val remoteControlClient = lazy {
			// build the PendingIntent for the remote control client
			val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
			mediaButtonIntent.component = remoteControlReceiver.value
			val mediaPendingIntent = PendingIntent.getBroadcast(this@PlaybackService, 0, mediaButtonIntent, 0)
			// create and register the remote control client
			RemoteControlClient(mediaPendingIntent)
		}
	private val lazyMediaSession = object : AbstractSynchronousLazy<MediaSessionCompat>() {
		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
		override fun create(): MediaSessionCompat {
			val newMediaSession = MediaSessionCompat(
				this@PlaybackService,
				mediaSessionTag)
			newMediaSession.setCallback(MediaSessionCallbackReceiver(this@PlaybackService))
			val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
			mediaButtonIntent.component = remoteControlReceiver.value
			val mediaPendingIntent = PendingIntent.getBroadcast(this@PlaybackService, 0, mediaButtonIntent, 0)
			newMediaSession.setMediaButtonReceiver(mediaPendingIntent)
			return newMediaSession
		}
	}
	private val lazyPlaybackBroadcaster = lazy { LocalPlaybackBroadcaster(localBroadcastManagerLazy.value) }
	private val lazyChosenLibraryIdentifierProvider = lazy { SelectedBrowserLibraryIdentifierProvider(this) }
	private val lazyPlaybackStartedBroadcaster = lazy { PlaybackStartedBroadcaster(localBroadcastManagerLazy.value) }
	private val lazyLibraryRepository = lazy { LibraryRepository(this) }
	private val lazyPlaylistVolumeManager = lazy { PlaylistVolumeManager(1.0f) }
	private val lazyVolumeLevelSettings = lazy { VolumeLevelSettings(this) }
	private val lazyChannelConfiguration = lazy { SharedChannelProperties(this) }
	private val lazyPlaybackNotificationsConfiguration = lazy {
			val notificationChannelActivator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManagerLazy.value) else NoOpChannelActivator()
			val channelName = notificationChannelActivator.activateChannel(lazyChannelConfiguration.value)
			NotificationsConfiguration(channelName, playingNotificationId)
		}
	private val lazyMediaStyleNotificationSetup = lazy {
			MediaStyleNotificationSetup(
				this@PlaybackService,
				NotificationBuilderProducer(this@PlaybackService),
				lazyPlaybackNotificationsConfiguration.value,
				lazyMediaSession.getObject())
		}
	private val lazyAllStoredFilesInLibrary = lazy { StoredFilesCollection(this) }
	private val extractorThread = lazy {
			HandlerThreadCreator.promiseNewHandlerThread(
				"Media Extracting thread",
				Process.THREAD_PRIORITY_AUDIO)
		}
	private val extractorHandler = lazy { extractorThread.value.then { h -> Handler(h.looper) } }
	private val lazyPlaybackStartingNotificationBuilder = lazy {
			PlaybackStartingNotificationBuilder(
				this@PlaybackService,
				NotificationBuilderProducer(this@PlaybackService),
				lazyPlaybackNotificationsConfiguration.value,
				lazyMediaSession.getObject())
		}
	private val lazySelectedLibraryProvider = lazy {
			SelectedBrowserLibraryProvider(
				SelectedBrowserLibraryIdentifierProvider(this@PlaybackService),
				LibraryRepository(this@PlaybackService))
	}
	private val lazyFileProperties = lazy {
		FilePropertiesProvider(
			get(this),
			FilePropertyCache.getInstance())
	}
	private val lazyCachedFileProperties = lazy {
		CachedFilePropertiesProvider(
			get(this),
			FilePropertyCache.getInstance(),
			lazyFileProperties.value)
	}
	private val lazyAudioRequest = lazy {
		AudioFocusRequestCompat
			.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setAudioAttributes(AudioAttributesCompat.Builder()
				.setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
				.setUsage(AudioAttributesCompat.USAGE_MEDIA)
				.build())
			.setOnAudioFocusChangeListener(this)
			.build()
	}
	private val lazyAudioBecomingNoisyReceiver = lazy { AudioBecomingNoisyReceiver() }
	private val lazyNotificationController = lazy { NotificationsController(this, notificationManagerLazy.value) }
	private val lazyDisconnectionTracker = lazy { ConnectionCircuitTracker() }
	private var areListenersRegistered = false
	private var playbackEnginePromise: Promise<PlaybackEngine>? = null
	private var playbackEngine: PlaybackEngine? = null
	private var playbackQueues: PreparedPlaybackQueueResourceManagement? = null
	private var cachedSessionFilePropertiesProvider: CachedSessionFilePropertiesProvider? = null
	private var positionedPlayingFile: PositionedPlayingFile? = null
	private var filePositionSubscription: Disposable? = null
	private var playlistPlaybackBootstrapper: PlaylistPlaybackBootstrapper? = null
	private var remoteControlProxy: RemoteControlProxy? = null
	private var playbackNotificationRouter: PlaybackNotificationRouter? = null
	private var nowPlayingNotificationBuilder: NowPlayingNotificationBuilder? = null
	private var wakeLock: WakeLock? = null
	private var cache: SimpleCache? = null
	private var startId = 0

	private val connectionRegainedListener = lazy { VoidResponse<IConnectionProvider> { resumePlayback() } }
	private val onPollingCancelledListener = lazy {
		VoidResponse<Throwable?> { e ->
			if (e is CancellationException) {
				unregisterListeners()
				stopSelf(startId)
			}
		}
	}
	private val onLibraryChanged: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val chosenLibrary = intent.getIntExtra(LibrarySelectionKey.chosenLibraryKey, -1)
			if (chosenLibrary < 0) return
			pausePlayback(true)
			stopSelf(startId)
		}
	}
	private val onPlaybackEngineChanged: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			pausePlayback(true)
			stopSelf(startId)
		}
	}
	private val buildSessionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val buildStatus = intent.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1)
			handleBuildConnectionStatusChange(buildStatus)
		}
	}
	private val unhandledRejectionHandler = ImmediateResponse<Throwable, Unit>(::uncaughtExceptionHandler)

	private fun stopNotificationIfNotPlaying() {
		if (!isPlaying) lazyNotificationController.value.removeNotification(playingNotificationId)
	}

	private fun notifyStartingService(): Promise<Unit> {
		return lazyPlaybackStartingNotificationBuilder.value
			.promisePreparedPlaybackStartingNotification()
			.then { b ->
				lazyNotificationController.value.notifyForeground(b.build(), startingNotificationId)
			}
	}

	private fun registerListeners() {
		AudioManagerCompat.requestAudioFocus(audioManagerLazy.value, lazyAudioRequest.value)
		wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, MediaPlayer::class.java.name)
		wakeLock?.acquire()
		registerRemoteClientControl()
		registerReceiver(
			lazyAudioBecomingNoisyReceiver.value,
			IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
		areListenersRegistered = true
	}

	private fun registerRemoteClientControl() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			lazyMediaSession.getObject().isActive = true
			return
		}
		audioManagerLazy.value.registerMediaButtonEventReceiver(remoteControlReceiver.value)
		audioManagerLazy.value.registerRemoteControlClient(remoteControlClient.value)
	}

	private fun unregisterListeners() {
		AudioManagerCompat.abandonAudioFocusRequest(audioManagerLazy.value, lazyAudioRequest.value)

		wakeLock?.apply { if (isHeld) release() }
		wakeLock = null

		if (lazyAudioBecomingNoisyReceiver.isInitialized()) unregisterReceiver(lazyAudioBecomingNoisyReceiver.value)
		areListenersRegistered = false
	}

	/* Begin Event Handlers */
	override fun onCreate() {
		registerRemoteClientControl()
		localBroadcastManagerLazy.value
			.registerReceiver(
				onPlaybackEngineChanged,
				IntentFilter(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeChanged))
		localBroadcastManagerLazy.value
			.registerReceiver(
				onLibraryChanged,
				IntentFilter(BrowserLibrarySelection.libraryChosenEvent))
	}

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		// Should be modified to save its state locally in the future.
		this.startId = startId
		if (intent.action == null) {
			stopSelf(startId)
			return START_NOT_STICKY
		}

		val action = intent.action
		if (Action.killMusicService == action || !Action.validActions.contains(action)) {
			stopSelf(startId)
			return START_NOT_STICKY
		}

		if (playbackEngine != null) {
			actOnIntent(intent).excuse(unhandledRejectionHandler)
			return START_NOT_STICKY
		}

		notifyStartingService()
			.eventually { lazySelectedLibraryProvider.value.browserLibrary }
			.eventually {
				if (it != null)
					initializePlaybackPlaylistStateManagerSerially(it)
				else
					Promise.empty()
			}
			.eventually { actOnIntent(intent) }
			.must { lazyNotificationController.value.removeNotification(startingNotificationId) }
			.excuse(unhandledRejectionHandler)
		return START_NOT_STICKY
	}

	private fun actOnIntent(intent: Intent?): Promise<Unit> {
		if (intent == null) return Unit.toPromise()
		var action = intent.action ?: return Unit.toPromise()
		val playbackEngine = playbackEngine ?: return Unit.toPromise()

		if (action == Action.togglePlayPause) action = if (isPlaying) Action.pause else Action.play
		if (!Action.playbackStartingActions.contains(action)) stopNotificationIfNotPlaying()
		when (action) {
			Action.launchMusicService -> {
				val playlistPosition = intent.getIntExtra(Bag.playlistPosition, -1)
				if (playlistPosition < 0) return Unit.toPromise()
				val playlistString = intent.getStringExtra(Bag.filePlaylist) ?: return Unit.toPromise()

				return FileStringListUtilities
					.promiseParsedFileStringList(playlistString)
					.eventually { playlist ->
						val promiseStartedPlaylist = playbackEngine.startPlaylist(
							playlist.toMutableList(),
							playlistPosition,
							0)
						startNowPlayingActivity(this)
						promiseStartedPlaylist
					}
					.then { localBroadcastManagerLazy.value.sendBroadcast(Intent(PlaylistEvents.onPlaylistChange)) }
			}
			Action.play -> {
				return resumePlayback()
			}
			Action.pause -> return pausePlayback(true)
			Action.repeating -> {
				playbackEngine.playRepeatedly()
				return Unit.toPromise()
			}
			Action.completing -> {
				playbackEngine.playToCompletion()
				return Unit.toPromise()
			}
			Action.seekTo -> {
				val playlistPosition = intent.getIntExtra(Bag.playlistPosition, -1)
				if (playlistPosition < 0) return Unit.toPromise()

				val filePosition = intent.getIntExtra(Bag.startPos, -1)
				if (filePosition < 0) return Unit.toPromise()
				return playbackEngine
					.changePosition(playlistPosition, filePosition)
					.then { positionedFile -> broadcastChangedFile(positionedFile) }
			}
			Action.previous -> {
				return playbackEngine
					.skipToPrevious()
					.then { positionedFile -> broadcastChangedFile(positionedFile) }
			}
			Action.next -> {
				return playbackEngine
					.skipToNext()
					.then { positionedFile -> broadcastChangedFile(positionedFile) }
			}
			Action.addFileToPlaylist -> {
				val fileKey = intent.getIntExtra(Bag.playlistPosition, -1)
				return if (fileKey < 0) Unit.toPromise() else playbackEngine
					.addFile(ServiceFile(fileKey))
					.then { localBroadcastManagerLazy.value.sendBroadcast(Intent(PlaylistEvents.onPlaylistChange)) }
					.eventually(LoopedInPromise.response({
						Toast.makeText(this, getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show()
					}, this))
			}
			Action.removeFileAtPositionFromPlaylist -> {
				val filePosition = intent.getIntExtra(Bag.filePosition, -1)
				return if (filePosition < -1) Unit.toPromise() else playbackEngine
					.removeFileAtPosition(filePosition)
					.then {
						localBroadcastManagerLazy.value.sendBroadcast(Intent(PlaylistEvents.onPlaylistChange))
					}
					.unitResponse()
			}
			else -> return Unit.toPromise()
		}
	}

	@Synchronized
	private fun initializePlaybackPlaylistStateManagerSerially(library: Library): Promise<PlaybackEngine> {
		return playbackEnginePromise?.eventually(
			{ initializePlaybackEngine(library) },
			{ initializePlaybackEngine(library) })
			?: initializePlaybackEngine(library).also { playbackEnginePromise = it }
	}

	private fun initializePlaybackEngine(library: Library): Promise<PlaybackEngine> {
		playbackEngine?.close()

		return sessionConnection.eventually { connectionProvider ->
			if (connectionProvider == null) throw PlaybackEngineInitializationException("connectionProvider was null!")

			cachedSessionFilePropertiesProvider = CachedSessionFilePropertiesProvider(
				connectionProvider,
				FilePropertyCache.getInstance(),
				SessionFilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance()))

			val imageProvider = ImageProvider(
				StaticLibraryIdentifierProvider(lazyChosenLibraryIdentifierProvider.value),
				MemoryCachedImageAccess.getInstance(this))

			remoteControlProxy?.also(localBroadcastManagerLazy.value::unregisterReceiver)
			val broadcaster = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) MediaSessionBroadcaster(
				this,
				cachedSessionFilePropertiesProvider,
				imageProvider,
				lazyMediaSession.getObject())
			else RemoteControlClientBroadcaster(
				this,
				cachedSessionFilePropertiesProvider,
				imageProvider,
				remoteControlClient.value)
			RemoteControlProxy(broadcaster)
				.also { rcp ->
					remoteControlProxy = rcp
					localBroadcastManagerLazy
						.value
						.registerReceiver(
							rcp,
							buildRemoteControlProxyIntentFilter(rcp))
				}

			nowPlayingNotificationBuilder?.close()
			NowPlayingNotificationBuilder(
				this,
				lazyMediaStyleNotificationSetup.value,
				connectionProvider,
				cachedSessionFilePropertiesProvider,
				imageProvider)
				.also { nowPlayingNotificationBuilder = it }
				.let { builder ->
					playbackNotificationRouter?.also(localBroadcastManagerLazy.value::unregisterReceiver)
					PlaybackNotificationRouter(PlaybackNotificationBroadcaster(
						lazyNotificationController.value,
						lazyPlaybackNotificationsConfiguration.value,
						builder,
						lazyPlaybackStartingNotificationBuilder.value))
				}
				.also { router ->
					playbackNotificationRouter = router

					localBroadcastManagerLazy
						.value
						.registerReceiver(router, buildNotificationRouterIntentFilter(router))
				}

			val cacheConfiguration = AudioCacheConfiguration(library)

			cache?.release()
			val cacheDirectoryProvider = AndroidDiskCacheDirectoryProvider(this).getDiskCacheDirectory(cacheConfiguration)
			val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheConfiguration.maxSize)
			SimpleCache(cacheDirectoryProvider, cacheEvictor)
				.also { cache = it }
				.let { simpleCache ->
					val arbitratorForOs = ExternalStorageReadPermissionsArbitratorForOs(this)
					val remoteFileUriProvider = RemoteFileUriProvider(connectionProvider, ServiceFileUriQueryParamsProvider())
					val bestMatchUriProvider = BestMatchUriProvider(
						library,
						StoredFileUriProvider(
							lazySelectedLibraryProvider.value,
							StoredFileAccess(this, lazyAllStoredFilesInLibrary.value),
							arbitratorForOs),
						CachedAudioFileUriProvider(
							remoteFileUriProvider,
							CachedFilesProvider(this, cacheConfiguration)),
						MediaFileUriProvider(
							this,
							MediaQueryCursorProvider(this, lazyCachedFileProperties.value),
							arbitratorForOs,
							lazyChosenLibraryIdentifierProvider.value,
							false),
						remoteFileUriProvider)

					extractorHandler.value.then { handler ->
						val playbackEngineBuilder = PreparedPlaybackQueueFeederBuilder(
							this,
							handler,
							MediaSourceProvider(
								library,
								HttpDataSourceFactoryProvider(this, connectionProvider, OkHttpFactory.getInstance()),
								simpleCache),
							bestMatchUriProvider)

						MaxFileVolumePreparationProvider(
							playbackEngineBuilder.build(library),
							MaxFileVolumeProvider(
								lazyVolumeLevelSettings.value,
								cachedSessionFilePropertiesProvider))
					}
				}
			}
			.eventually { preparationSourceProvider ->
				playbackQueues?.close()
				PreparedPlaybackQueueResourceManagement(preparationSourceProvider, preparationSourceProvider)
					.also { playbackQueues = it }
					.let { queues ->
						playlistPlaybackBootstrapper?.close()
						PlaylistPlaybackBootstrapper(lazyPlaylistVolumeManager.value)
							.also { playlistPlaybackBootstrapper = it }
							.let { bootstrapper ->
								val nowPlayingRepository = NowPlayingRepository(
									SpecificLibraryProvider(
										lazyChosenLibraryIdentifierProvider.value.selectedLibraryId!!,
										lazyLibraryRepository.value),
									lazyLibraryRepository.value)

								createEngine(
									queues,
									QueueProviders.providers(),
									nowPlayingRepository,
									bootstrapper)
							}
					}
			}
			.then { engine ->
				playbackEngine = engine
				engine
					.setOnPlaybackStarted(::handlePlaybackStarted)
					.setOnPlayingFileChanged(::changePositionedPlaybackFile)
					.setOnPlaylistError(::uncaughtExceptionHandler)
					.setOnPlaybackCompleted(::onPlaylistPlaybackComplete)
					.setOnPlaylistReset(::broadcastResetPlaylist)
			}
	}

	private val sessionConnection: Promise<IConnectionProvider>
		get() {
			localBroadcastManagerLazy.value
				.registerReceiver(
					buildSessionReceiver,
					IntentFilter(SessionConnection.buildSessionBroadcast))
			return SessionConnection.getInstance(this).promiseSessionConnection().must {
				localBroadcastManagerLazy.value.unregisterReceiver(buildSessionReceiver)
				lazyNotificationController.value.removeNotification(connectingNotificationId)
			}
		}

	private fun handleBuildConnectionStatusChange(status: Int) {
		val notifyBuilder = NotificationCompat.Builder(this, lazyPlaybackNotificationsConfiguration.value.notificationChannel)
		notifyBuilder
			.setOngoing(false)
			.setContentTitle(getText(R.string.title_svc_connecting_to_server))
		when (status) {
			BuildingSessionConnectionStatus.GettingLibrary -> notifyBuilder.setContentText(getText(R.string.lbl_getting_library_details))
			BuildingSessionConnectionStatus.GettingLibraryFailed -> {
				Toast.makeText(this, getText(R.string.lbl_please_connect_to_valid_server), Toast.LENGTH_SHORT).show()
				return
			}
			BuildingSessionConnectionStatus.SendingWakeSignal -> notifyBuilder.setContentText(getString(R.string.sending_wake_signal))
			BuildingSessionConnectionStatus.BuildingConnection -> notifyBuilder.setContentText(getText(R.string.lbl_connecting_to_server_library))
			BuildingSessionConnectionStatus.BuildingConnectionFailed -> {
				Toast.makeText(this, getText(R.string.lbl_error_connecting_try_again), Toast.LENGTH_SHORT).show()
				return
			}
			else -> return
		}
		lazyNotificationController.value.notifyForeground(
			buildFullNotification(notifyBuilder),
			connectingNotificationId)
	}

	private fun handlePlaybackStarted() {
		isPlaying = true
		lazyPlaybackStartedBroadcaster.value.broadcastPlaybackStarted()
	}

	private fun resumePlayback(): Promise<Unit> = playbackEngine?.resume()?.then {
		isPlaying = true
		if (!areListenersRegistered) registerListeners()
	} ?: Unit.toPromise()

	private fun pausePlayback(isUserInterrupted: Boolean): Promise<Unit> {
		isPlaying = false
		if (isUserInterrupted && areListenersRegistered) unregisterListeners()
		return playbackEngine?.pause()
			?.then {
				positionedPlayingFile?.apply {
					lazyPlaybackBroadcaster.value
						.sendPlaybackBroadcast(
							PlaylistEvents.onPlaylistPause,
							lazyChosenLibraryIdentifierProvider.value.selectedLibraryId,
							asPositionedFile())
				}

				filePositionSubscription?.dispose()
			}
			?: Unit.toPromise()
	}

	private fun uncaughtExceptionHandler(exception: Throwable?) {
		when (exception) {
			is PlaybackEngineInitializationException -> handlePlaybackEngineInitializationException(exception)
			is PreparationException -> handlePreparationException(exception)
			is IOException -> handleIoException(exception)
			is ExoPlaybackException -> handleExoPlaybackException(exception)
			is PlaybackException -> handlePlaybackException(exception)
			else -> {
				logger.error("An unexpected error has occurred!", exception)
				lazyNotificationController.value.removeAllNotifications()
			}
		}
	}

	private fun handlePlaybackEngineInitializationException(exception: PlaybackEngineInitializationException) {
		logger.error("There was an error initializing the playback engine", exception)
		lazyNotificationController.value.removeAllNotifications()
	}

	private fun handlePreparationException(preparationException: PreparationException) {
		logger.error("An error occurred during file preparation for file " + preparationException.positionedFile.serviceFile, preparationException)
		uncaughtExceptionHandler(preparationException.cause)
	}

	private fun handlePlaybackException(exception: PlaybackException) {
		when (val cause = exception.cause) {
			is ExoPlaybackException -> handleExoPlaybackException(cause)
			is IllegalStateException -> {
				logger.error("The player ended up in an illegal state - closing and restarting the player", exception)
				closeAndRestartPlaylistManager()
			}
			is IOException -> handleIoException(cause)
			null -> logger.error("An unexpected playback exception occurred", exception)
			else -> uncaughtExceptionHandler(cause)
		}
	}

	private fun handleExoPlaybackException(exception: ExoPlaybackException) {
		logger.error("An ExoPlaybackException occurred")
		val cause = exception.cause
		if (cause is IllegalStateException) {
			logger.error("The ExoPlayer player ended up in an illegal state, closing and restarting the player", cause)
			closeAndRestartPlaylistManager()
			return
		}
		if (cause != null) uncaughtExceptionHandler(exception.cause)
	}

	private fun handleIoException(exception: IOException?) {
		if (exception is InvalidResponseCodeException && exception.responseCode == 416) {
			logger.warn("Received an error code of " + exception.responseCode + ", will attempt restarting the player", exception)
			closeAndRestartPlaylistManager()
			return
		}

		logger.error("An IO exception occurred during playback", exception)
		handleDisconnection()
	}

	private fun handleDisconnection() {
		if (!lazyDisconnectionTracker.value.isConnectionPastThreshold()) return
		pollSessionConnection(this, true)
			.then(connectionRegainedListener.value, onPollingCancelledListener.value)
	}

	private fun closeAndRestartPlaylistManager() {
		try {
			playbackEngine?.close()
		} catch (e: Exception) {
			uncaughtExceptionHandler(e)
			return
		}

		lazyLibraryRepository.value
			.getLibrary(lazyChosenLibraryIdentifierProvider.value.selectedLibraryId!!)
			.eventually { library ->
				if (library != null)
					initializePlaybackPlaylistStateManagerSerially(library)
				else
					Promise.empty()
			}
			.then { if (isPlaying) resumePlayback() }
			.excuse(unhandledRejectionHandler)
	}

	override fun onAudioFocusChange(focusChange: Int) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// resume playback
			if (lazyPlaylistVolumeManager.isInitialized()) lazyPlaylistVolumeManager.value.setVolume(1.0f)
			playbackEngine?.apply { if (!isPlaying) resumePlayback() }
			return
		}

		val isPlaying = playbackEngine?.isPlaying ?: false
		if (!isPlaying) return

		when (focusChange) {
			AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
				// Lost focus but it will be regained... cannot release resources
				pausePlayback(false)
				return
			}
			AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->                // Lost focus for a short time, but it's ok to keep playing
				// at an attenuated level
				if (lazyPlaylistVolumeManager.isInitialized()) lazyPlaylistVolumeManager.value.setVolume(0.2f)
		}
	}

	private fun changePositionedPlaybackFile(positionedPlayingFile: PositionedPlayingFile) {
		this.positionedPlayingFile = positionedPlayingFile

		val playingFile = positionedPlayingFile.playingFile
		filePositionSubscription?.dispose()

		if (playingFile is EmptyPlaybackHandler) return

		broadcastChangedFile(positionedPlayingFile.asPositionedFile())
		lazyPlaybackBroadcaster.value.sendPlaybackBroadcast(PlaylistEvents.onPlaylistTrackStart, lazyChosenLibraryIdentifierProvider.value.selectedLibraryId, positionedPlayingFile.asPositionedFile())
		val promisedPlayedFile = playingFile.promisePlayedFile()
		val localSubscription = Observable.interval(1, TimeUnit.SECONDS, lazyObservationScheduler.value)
			.map { promisedPlayedFile.progress }
			.distinctUntilChanged()
			.subscribe(TrackPositionBroadcaster(
				localBroadcastManagerLazy.value,
				playingFile))

		promisedPlayedFile.then {
			lazyPlaybackBroadcaster.value.sendPlaybackBroadcast(PlaylistEvents.onPlaylistTrackComplete, lazyChosenLibraryIdentifierProvider.value.selectedLibraryId, positionedPlayingFile.asPositionedFile())
			localSubscription?.dispose()
		}

		filePositionSubscription = localSubscription

		if (!areListenersRegistered) registerListeners()
		registerRemoteClientControl()
	}

	private fun broadcastResetPlaylist(positionedFile: PositionedFile) {
		lazyPlaybackBroadcaster.value
			.sendPlaybackBroadcast(
				PlaylistEvents.onPlaylistTrackChange,
				lazyChosenLibraryIdentifierProvider.value.selectedLibraryId,
				positionedFile)
	}

	private fun broadcastChangedFile(positionedFile: PositionedFile) {
		lazyPlaybackBroadcaster.value.sendPlaybackBroadcast(PlaylistEvents.onPlaylistTrackChange, lazyChosenLibraryIdentifierProvider.value.selectedLibraryId, positionedFile)
	}

	private fun onPlaylistPlaybackComplete() {
		lazyPlaybackBroadcaster.value.sendPlaybackBroadcast(PlaylistEvents.onPlaylistStop, lazyChosenLibraryIdentifierProvider.value.selectedLibraryId, positionedPlayingFile!!.asPositionedFile())
		killService(this)
	}

	override fun onDestroy() {
		if (lazyNotificationController.isInitialized()) lazyNotificationController.value.removeAllNotifications()

		try {
			playlistPlaybackBootstrapper?.close()
		} catch (e: IOException) {
			logger.warn("There was an error closing the prepared playback bootstrapper", e)
		}

		try {
			playbackEngine?.close()
		} catch (e: Exception) {
			logger.warn("There was an error closing the playback engine", e)
		}

		try {
			playbackQueues?.close()
		} catch (e: Exception) {
			logger.warn("There was an error closing the prepared playback queue", e)
		}

		if (areListenersRegistered) unregisterListeners()
		if (remoteControlReceiver.isInitialized()) audioManagerLazy.value.unregisterMediaButtonEventReceiver(remoteControlReceiver.value)
		if (remoteControlClient.isInitialized()) audioManagerLazy.value.unregisterRemoteControlClient(remoteControlClient.value)
		if (extractorThread.isInitialized()) extractorThread.value.then { it.quitSafely() }

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && lazyMediaSession.isCreated) {
			lazyMediaSession.getObject().isActive = false
			lazyMediaSession.getObject().release()
		}

		filePositionSubscription?.dispose()
		cache?.release()
		nowPlayingNotificationBuilder?.close()

		if (!localBroadcastManagerLazy.isInitialized()) return

		localBroadcastManagerLazy.value.unregisterReceiver(buildSessionReceiver)
		localBroadcastManagerLazy.value.unregisterReceiver(onLibraryChanged)
		localBroadcastManagerLazy.value.unregisterReceiver(onPlaybackEngineChanged)

		remoteControlProxy?.also(localBroadcastManagerLazy.value::unregisterReceiver)
		playbackNotificationRouter?.also(localBroadcastManagerLazy.value::unregisterReceiver)
	}

	/* End Event Handlers */ /* Begin Binder Code */
	override fun onBind(intent: Intent): IBinder? {
		return lazyBinder.value
	}

	/* End Binder Code */
	private object Action {
		private val magicPropertyBuilder = MagicPropertyBuilder(Action::class.java)

		/* String constant actions */
		val launchMusicService = magicPropertyBuilder.buildProperty("launchMusicService")
		val play = magicPropertyBuilder.buildProperty("play")
		val pause = magicPropertyBuilder.buildProperty("pause")
		val togglePlayPause = magicPropertyBuilder.buildProperty("togglePlayPause")
		val repeating = magicPropertyBuilder.buildProperty("repeating")
		val completing = magicPropertyBuilder.buildProperty("completing")
		val previous = magicPropertyBuilder.buildProperty("previous")
		val next = magicPropertyBuilder.buildProperty("then")
		val seekTo = magicPropertyBuilder.buildProperty("seekTo")
		val addFileToPlaylist = magicPropertyBuilder.buildProperty("addFileToPlaylist")
		val removeFileAtPositionFromPlaylist = magicPropertyBuilder.buildProperty("removeFileAtPositionFromPlaylist")
		val killMusicService = magicPropertyBuilder.buildProperty("killMusicService")
		val validActions = setOf(launchMusicService,
			play,
			pause,
			togglePlayPause,
			previous,
			next,
			seekTo,
			repeating,
			completing,
			addFileToPlaylist,
			removeFileAtPositionFromPlaylist)
		val playbackStartingActions = setOf(launchMusicService, play, togglePlayPause)

		object Bag {
			private val magicPropertyBuilder = MagicPropertyBuilder(Bag::class.java)

			/* Bag constants */
			val playlistPosition = magicPropertyBuilder.buildProperty("playlistPosition")
			val filePlaylist = magicPropertyBuilder.buildProperty("filePlaylist")
			val startPos = magicPropertyBuilder.buildProperty("startPos")
			val filePosition = magicPropertyBuilder.buildProperty("filePosition")
		}
	}
}
