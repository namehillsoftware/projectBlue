package com.lasthopesoftware.bluewater.client.playback.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.Process
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.pollSessionConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.BuildingSessionConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.engine.*
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
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.LocalPlaybackBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaybackStartedBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.MediaStyleNotificationSetup
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.NowPlayingNotificationBuilder
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.PlaybackStartingNotificationBuilder
import com.lasthopesoftware.bluewater.client.playback.service.receivers.AudioBecomingNoisyReceiver
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.RemoteControlProxy
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.NowPlayingActivity.Companion.startNowPlayingActivity
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.settings.volumeleveling.VolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.GenericBinder
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.MediaSession.MediaSessionService
import com.lasthopesoftware.bluewater.shared.android.audiofocus.AudioFocusManagement
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.NotificationBuilderProducer
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.SharedChannelProperties
import com.lasthopesoftware.bluewater.shared.android.services.promiseBoundService
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.makePendingIntentImmutable
import com.lasthopesoftware.bluewater.shared.observables.toMaybeObservable
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay.Companion.delay
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.bluewater.shared.resilience.TimedCountdownLatch
import com.lasthopesoftware.resources.closables.CloseableManager
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.loopers.HandlerThreadCreator
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

open class PlaybackService : Service() {

	companion object {
		private val logger = LoggerFactory.getLogger(PlaybackService::class.java)

		private const val playingNotificationId = 42
		private const val connectingNotificationId = 70

		private const val numberOfDisconnects = 3
		private val disconnectResetDuration = Duration.standardSeconds(1)

		private const val numberOfErrors = 5
		private val errorLatchResetDuration = Duration.standardSeconds(3)

		private val playbackStartTimeout = Duration.standardMinutes(2)

		fun initialize(context: Context) =
			context.safelyStartService(getNewSelfIntent(context, Action.initialize))

		fun launchMusicService(context: Context, serializedFileList: String?) =
			launchMusicService(context, 0, serializedFileList)

		fun launchMusicService(context: Context, filePos: Int, serializedFileList: String?) {
			val svcIntent = getNewSelfIntent(context, Action.launchMusicService)
			svcIntent.putExtra(Bag.playlistPosition, filePos)
			svcIntent.putExtra(Bag.filePlaylist, serializedFileList)
			context.safelyStartServiceInForeground(svcIntent)
		}

		@JvmOverloads
		@JvmStatic
		fun seekTo(context: Context, filePos: Int, fileProgress: Int = 0) {
			val svcIntent = getNewSelfIntent(context, Action.seekTo)
			svcIntent.putExtra(Bag.playlistPosition, filePos)
			svcIntent.putExtra(Bag.startPos, fileProgress)
			context.safelyStartService(svcIntent)
		}

		fun play(context: Context) = context.safelyStartServiceInForeground(getNewSelfIntent(context, Action.play))

		fun pendingPlayingIntent(context: Context): PendingIntent =
			PendingIntent.getService(
				context,
				0,
				getNewSelfIntent(
					context,
					Action.play),
				PendingIntent.FLAG_UPDATE_CURRENT.makePendingIntentImmutable())

		@JvmStatic
		fun pause(context: Context) = context.safelyStartService(getNewSelfIntent(context, Action.pause))

		@JvmStatic
		fun pendingPauseIntent(context: Context): PendingIntent =
			PendingIntent.getService(
				context,
				0,
				getNewSelfIntent(context, Action.pause),
				PendingIntent.FLAG_UPDATE_CURRENT.makePendingIntentImmutable())

		fun togglePlayPause(context: Context) = context.safelyStartService(getNewSelfIntent(context, Action.togglePlayPause))

		fun next(context: Context) = context.safelyStartService(getNewSelfIntent(context, Action.next))

		fun pendingNextIntent(context: Context): PendingIntent =
			PendingIntent.getService(
				context,
				0,
				getNewSelfIntent(context, Action.next),
				PendingIntent.FLAG_UPDATE_CURRENT.makePendingIntentImmutable())

		fun previous(context: Context) = context.safelyStartService(getNewSelfIntent(context, Action.previous))

		fun pendingPreviousIntent(context: Context): PendingIntent =
			PendingIntent.getService(
				context,
				0,
				getNewSelfIntent(context, Action.previous),
				PendingIntent.FLAG_UPDATE_CURRENT.makePendingIntentImmutable())

		fun setRepeating(context: Context) = context.safelyStartService(getNewSelfIntent(context, Action.repeating))

		fun setCompleting(context: Context) = context.safelyStartService(getNewSelfIntent(context, Action.completing))

		fun addFileToPlaylist(context: Context, fileKey: Int) {
			val intent = getNewSelfIntent(context, Action.addFileToPlaylist)
			intent.putExtra(Bag.playlistPosition, fileKey)
			context.safelyStartService(intent)
		}

		fun removeFileAtPositionFromPlaylist(context: Context, filePosition: Int) {
			val intent = getNewSelfIntent(context, Action.removeFileAtPositionFromPlaylist)
			intent.putExtra(Bag.filePosition, filePosition)
			context.safelyStartService(intent)
		}

		fun killService(context: Context) =
			context.safelyStartService(getNewSelfIntent(context, Action.killMusicService))

		fun pendingKillService(context: Context): PendingIntent =
			PendingIntent.getService(
				context,
				0,
				getNewSelfIntent(context, Action.killMusicService),
				PendingIntent.FLAG_UPDATE_CURRENT.makePendingIntentImmutable())

		fun promiseIsMarkedForPlay(context: Context): Promise<Boolean> =
			context.promiseBoundService<PlaybackService>()
				.then { h ->
					val isPlaying = h.service.isMarkedForPlay
					context.unbindService(h.serviceConnection)
					isPlaying
				}

		private fun getNewSelfIntent(context: Context, action: String): Intent {
			val newIntent = Intent(context, PlaybackService::class.java)
			newIntent.action = action
			return newIntent
		}

		private fun Context.safelyStartService(intent: Intent) {
			try {
				startService(intent)
			} catch (e: IllegalStateException) {
				logger.warn("An illegal state exception occurred while trying to start the service", e)
			} catch (e: SecurityException) {
				logger.warn("A security exception occurred while trying to start the service", e)
			}
		}

		private fun Context.safelyStartServiceInForeground(intent: Intent) {
			try {
				ContextCompat.startForegroundService(this, intent)
			} catch (e: IllegalStateException) {
				logger.warn("An illegal state exception occurred while trying to start the service", e)
			} catch (e: SecurityException) {
				logger.warn("A security exception occurred while trying to start the service", e)
			}
		}

		private fun buildFullNotification(notificationBuilder: NotificationCompat.Builder) =
			notificationBuilder
				.setSmallIcon(R.drawable.clearstream_logo_dark)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.build()

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

	private val lazyObservationScheduler = lazy { ExecutorScheduler(ThreadPools.compute, true) }
	private val binder by lazy { GenericBinder(this) }
	private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
	private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
	private val localBroadcastManagerLazy = lazy { LocalBroadcastManager.getInstance(this) }
	private val lazyMessageBus = lazy { MessageBus(localBroadcastManagerLazy.value) }
	private val playbackBroadcaster by lazy { LocalPlaybackBroadcaster(lazyMessageBus.value) }
	private val applicationSettings by lazy { getApplicationSettingsRepository() }
	private val selectedLibraryIdentifierProvider by lazy { SelectedBrowserLibraryIdentifierProvider(applicationSettings) }
	private val playbackStartedBroadcaster by lazy { PlaybackStartedBroadcaster(localBroadcastManagerLazy.value) }
	private val libraryRepository by lazy { LibraryRepository(this) }
	private val playlistVolumeManager by lazy { PlaylistVolumeManager(1.0f) }
	private val volumeLevelSettings by lazy { VolumeLevelSettings(applicationSettings) }
	private val channelConfiguration by lazy { SharedChannelProperties(this) }

	private val playbackNotificationsConfiguration by lazy {
		val notificationChannelActivator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManager) else NoOpChannelActivator()
		val channelName = notificationChannelActivator.activateChannel(channelConfiguration)
		NotificationsConfiguration(channelName, playingNotificationId)
	}

	private val arbitratorForOs by lazy { ExternalStorageReadPermissionsArbitratorForOs(this) }

	private val lazyMediaSessionService = lazy { promiseBoundService<MediaSessionService>() }

	private val promisedMediaSession by lazy { lazyMediaSessionService.value.then { c -> c.service.mediaSession } }

	private val mediaStyleNotificationSetup by lazy {
			promisedMediaSession.then { mediaSession ->
				MediaStyleNotificationSetup(
					this,
					NotificationBuilderProducer(this),
					playbackNotificationsConfiguration,
					mediaSession
				)
			}
		}

	private val playbackThread = lazy {
		HandlerThreadCreator.promiseNewHandlerThread(
			"Playback",
			Process.THREAD_PRIORITY_AUDIO)
	}

	private val playbackHandler = lazy { playbackThread.value.then { h -> Handler(h.looper) } }

	private val playbackStartingNotificationBuilder by lazy {
		promisedMediaSession.then { mediaSession ->
			PlaybackStartingNotificationBuilder(
				this,
				NotificationBuilderProducer(this),
				playbackNotificationsConfiguration,
				mediaSession
			)
		}
	}

	private val selectedLibraryProvider by lazy {
		SelectedBrowserLibraryProvider(
			selectedLibraryIdentifierProvider,
			LibraryRepository(this))
	}

	private val fileProperties by lazy {
		val connectionSessionManager = ConnectionSessionManager.get(this)
		FilePropertiesProvider(
			connectionSessionManager,
			LibraryRevisionProvider(connectionSessionManager),
			FilePropertyCache.getInstance())
	}

	private val cachedFileProperties by lazy {
		CachedFilePropertiesProvider(
			ConnectionSessionManager.get(this),
			FilePropertyCache.getInstance(),
			fileProperties)
	}

	private val playbackEngineCloseables = CloseableManager()
	private val lazyAudioBecomingNoisyReceiver = lazy { AudioBecomingNoisyReceiver() }
	private val lazyNotificationController = lazy { NotificationsController(this, notificationManager) }
	private val disconnectionLatch by lazy { TimedCountdownLatch(numberOfDisconnects, disconnectResetDuration) }
	private val errorLatch by lazy { TimedCountdownLatch(numberOfErrors, errorLatchResetDuration) }

	private val connectionRegainedListener by lazy { ImmediateResponse<IConnectionProvider, Unit> { closeAndRestartPlaylistManager() } }
	private val onPollingCancelledListener by lazy { ImmediateResponse<Throwable?, Unit> { e ->
			if (e is CancellationException) {
				unregisterListeners()
				stopSelf(startId)
			}
		}
	}

	private val playbackHaltingEvent = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			pausePlayback()
			stopSelf(startId)
		}
	}

	private val buildSessionReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val buildStatus = intent.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1)
			handleBuildConnectionStatusChange(buildStatus)
		}
	}

	private val unhandledRejectionHandler = ImmediateResponse<Throwable, Unit>(::uncaughtExceptionHandler)

	private val sessionConnection: Promise<IConnectionProvider?>
		get() {
			localBroadcastManagerLazy.value
				.registerReceiver(
					buildSessionReceiver,
					IntentFilter(SelectedConnection.buildSessionBroadcast))
			return SelectedConnection.getInstance(this).promiseSessionConnection().must {
				localBroadcastManagerLazy.value.unregisterReceiver(buildSessionReceiver)
				lazyNotificationController.value.removeNotification(connectingNotificationId)
			}
		}

	private var isMarkedForPlay = false
	private var areListenersRegistered = false
	private var playbackEngineSync = Any()
	private var playbackEnginePromise = Promise.empty<PlaybackEngine?>()
	private var playbackContinuity: ChangePlaybackContinuity? = null
	private var playlistFiles: ChangePlaylistFiles? = null
	private var playbackState: ChangePlaybackState? = null
	private var playlistPosition: ChangePlaylistPosition? = null
	private var playbackQueues: PreparedPlaybackQueueResourceManagement? = null
	private var filePositionSubscription: Disposable? = null
	private var playlistPlaybackBootstrapper: PlaylistPlaybackBootstrapper? = null
	private var remoteControlProxy: RemoteControlProxy? = null
	private var playbackNotificationRouter: PlaybackNotificationRouter? = null
	private var nowPlayingNotificationBuilder: NowPlayingNotificationBuilder? = null
	private var wakeLock: WakeLock? = null
	private var cache: SimpleCache? = null
	private var startId = 0
	private var trackPositionBroadcaster: TrackPositionBroadcaster? = null

	private fun getNewNowPlayingRepository(): Promise<INowPlayingRepository?> =
		selectedLibraryIdentifierProvider.selectedLibraryId
			.then { l ->
				l?.let {
					NowPlayingRepository(
						SpecificLibraryProvider(l, libraryRepository),
						libraryRepository
					)
				}
			}

	private fun stopNotificationIfNotPlaying() {
		if (!isMarkedForPlay) lazyNotificationController.value.removeNotification(playingNotificationId)
	}

	private fun registerListeners() {
		wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, MediaPlayer::class.java.name)
		wakeLock?.acquire()
		registerReceiver(
			lazyAudioBecomingNoisyReceiver.value,
			IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
		areListenersRegistered = true
	}

	private fun unregisterListeners() {
		wakeLock?.apply { if (isHeld) release() }
		wakeLock = null

		if (lazyAudioBecomingNoisyReceiver.isInitialized()) unregisterReceiver(lazyAudioBecomingNoisyReceiver.value)
		areListenersRegistered = false
	}

	/* Begin Event Handlers */
	override fun onCreate() {
		val playbackHaltingIntentFilter = IntentFilter()
		playbackHaltingIntentFilter.addAction(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeChanged)
		playbackHaltingIntentFilter.addAction(BrowserLibrarySelection.libraryChosenEvent)
		playbackHaltingIntentFilter.addAction(SelectedConnectionSettingsChangeReceiver.connectionSettingsUpdated)

		localBroadcastManagerLazy.value.registerReceiver(playbackHaltingEvent,	playbackHaltingIntentFilter)
	}

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		fun actOnIntent(intent: Intent?): Promise<Unit> {
			if (intent == null) return Unit.toPromise()
			var action = intent.action ?: return Unit.toPromise()
			val playbackPosition = playlistPosition ?: return Unit.toPromise()

			if (action == Action.togglePlayPause) action = if (isMarkedForPlay) Action.pause else Action.play
			if (!Action.playbackStartingActions.contains(action)) stopNotificationIfNotPlaying()
			when (action) {
				Action.play -> return resumePlayback()
				Action.pause -> return pausePlayback()
				Action.repeating -> return playbackContinuity?.playRepeatedly() ?: Unit.toPromise()
				Action.completing -> return playbackContinuity?.playToCompletion() ?: Unit.toPromise()
				Action.previous -> return playbackPosition.skipToPrevious().then(::broadcastChangedFile)
				Action.next -> return playbackPosition.skipToNext().then(::broadcastChangedFile)
				Action.launchMusicService -> {
					val playlistPosition = intent.getIntExtra(Bag.playlistPosition, -1)
					if (playlistPosition < 0) return Unit.toPromise()

					val playlistString = intent.getStringExtra(Bag.filePlaylist) ?: return Unit.toPromise()

					return startNewPlaylist(playlistString, playlistPosition)
				}
				Action.seekTo -> {
					val playlistPosition = intent.getIntExtra(Bag.playlistPosition, -1)
					if (playlistPosition < 0) return Unit.toPromise()

					val filePosition = intent.getIntExtra(Bag.startPos, -1)
					if (filePosition < 0) return Unit.toPromise()
					return playbackPosition
						.changePosition(playlistPosition, Duration.millis(filePosition.toLong()))
						.then(::broadcastChangedFile)
				}
				Action.addFileToPlaylist -> {
					val playlistFiles = playlistFiles ?: return Unit.toPromise()

					val fileKey = intent.getIntExtra(Bag.playlistPosition, -1)
					return if (fileKey < 0) Unit.toPromise() else playlistFiles
						.addFile(ServiceFile(fileKey))
						.then { localBroadcastManagerLazy.value.sendBroadcast(Intent(PlaylistEvents.onPlaylistChange)) }
						.eventually(LoopedInPromise.response({
							Toast.makeText(this, getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show()
						}, this))
				}
				Action.removeFileAtPositionFromPlaylist -> {
					val playlistFiles = playlistFiles ?: return Unit.toPromise()

					val filePosition = intent.getIntExtra(Bag.filePosition, -1)
					return if (filePosition < -1) Unit.toPromise() else playlistFiles
						.removeFileAtPosition(filePosition)
						.then {
							localBroadcastManagerLazy.value.sendBroadcast(Intent(PlaylistEvents.onPlaylistChange))
						}
						.unitResponse()
				}
				Action.killMusicService -> return pausePlayback().must { stopSelf(startId) }
				else -> return Unit.toPromise()
			}
		}

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

		synchronized(playbackEngineSync) {
			playbackEnginePromise.must {
				if (playlistPosition != null) {
					actOnIntent(intent).excuse(unhandledRejectionHandler)
					return@must
				}

				val promisedTimeout = delay<Any?>(playbackStartTimeout)

				val promisedIntentHandling = selectedLibraryProvider.browserLibrary
					.eventually { it?.let(::initializePlaybackPlaylistStateManagerSerially) ?: Promise.empty() }
					.eventually { it?.let { actOnIntent(intent) } ?: Promise(UninitializedPlaybackEngineException()) }
					.must { promisedTimeout.cancel() }

				val timeoutResponse =
					promisedTimeout.then<Unit> { throw TimeoutException("Timed out after $playbackStartTimeout") }
				Promise.whenAny(promisedIntentHandling, timeoutResponse).excuse(unhandledRejectionHandler)
			}
		}

		return START_STICKY
	}

	private fun startNewPlaylist(playlistString: String, playlistPosition: Int): Promise<Unit> {
		val playbackState = playbackState ?: return Unit.toPromise()

		isMarkedForPlay = true
		return FileStringListUtilities
			.promiseParsedFileStringList(playlistString)
			.eventually { playlist ->
				val promiseStartedPlaylist = playbackState.startPlaylist(
					playlist.toMutableList(),
					playlistPosition,
					Duration.ZERO)
				startNowPlayingActivity(this)
				promiseStartedPlaylist
			}
			.then { localBroadcastManagerLazy.value.sendBroadcast(Intent(PlaylistEvents.onPlaylistChange)) }
	}

	private fun resumePlayback(): Promise<Unit> {
		isMarkedForPlay = true
		return playbackState?.resume()?.then {
			if (!areListenersRegistered) registerListeners()
		} ?: Unit.toPromise()
	}

	private fun pausePlayback(): Promise<Unit> {
		isMarkedForPlay = false
		if (areListenersRegistered) unregisterListeners()
		return playbackState?.pause() ?: Unit.toPromise()
	}

	private fun initializePlaybackPlaylistStateManagerSerially(library: Library): Promise<PlaybackEngine?> =
		synchronized(playbackEngineSync) {
			playbackEnginePromise.eventually(
				{ initializePlaybackEngine(library) },
				{ initializePlaybackEngine(library) }).also { playbackEnginePromise = it }
		}

	private fun initializePlaybackEngine(library: Library): Promise<PlaybackEngine?> {
		playbackEngineCloseables.close()

		return sessionConnection.eventually { connectionProvider ->
			if (connectionProvider == null) throw PlaybackEngineInitializationException("connectionProvider was null!")

			val scopedRevisionProvider = ScopedRevisionProvider(connectionProvider)

			val cachedSessionFilePropertiesProvider = ScopedCachedFilePropertiesProvider(
				connectionProvider,
				FilePropertyCache.getInstance(),
				ScopedFilePropertiesProvider(connectionProvider, scopedRevisionProvider, FilePropertyCache.getInstance()))

			trackPositionBroadcaster = TrackPositionBroadcaster(lazyMessageBus.value, cachedSessionFilePropertiesProvider)

			val imageProvider = CachedImageProvider.getInstance(this)

			remoteControlProxy?.also(localBroadcastManagerLazy.value::unregisterReceiver)

			val promisedMediaBroadcaster = promisedMediaSession.then { mediaSession ->
				val broadcaster = MediaSessionBroadcaster(
					this,
					cachedSessionFilePropertiesProvider,
					imageProvider,
					mediaSession)
				remoteControlProxy = RemoteControlProxy(broadcaster)
					.also { rcp ->
						localBroadcastManagerLazy
							.value
							.registerReceiver(rcp, buildRemoteControlProxyIntentFilter(rcp))
					}
			}

			val promisedMediaNotificationSetup = mediaStyleNotificationSetup.eventually { mediaStyleNotificationSetup ->
					NowPlayingNotificationBuilder(
						this,
						mediaStyleNotificationSetup,
						connectionProvider,
						cachedSessionFilePropertiesProvider,
						imageProvider
					)
					.also {
						playbackEngineCloseables.manage(it)
						nowPlayingNotificationBuilder = it
					}
					.let { builder ->
						playbackNotificationRouter?.also(localBroadcastManagerLazy.value::unregisterReceiver)
						playbackStartingNotificationBuilder.then { b ->
							b?.let { playbackStartingNotificationBuilder ->
								PlaybackNotificationRouter(
									PlaybackNotificationBroadcaster(
										lazyNotificationController.value,
										playbackNotificationsConfiguration,
										builder,
										playbackStartingNotificationBuilder
									)
								)
							}
						}
					}
					.then { router ->
						router?.also {
							playbackNotificationRouter = router

							localBroadcastManagerLazy
								.value
								.registerReceiver(router, buildNotificationRouterIntentFilter(router))
						}
					}
			}

			val cacheConfiguration = AudioCacheConfiguration(library)

			cache?.release()
			val cacheDirectoryProvider = AndroidDiskCacheDirectoryProvider(this).getDiskCacheDirectory(cacheConfiguration)
			val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheConfiguration.maxSize)
			SimpleCache(cacheDirectoryProvider, cacheEvictor)
				.also { cache = it }
				.let { simpleCache ->
					val remoteFileUriProvider = RemoteFileUriProvider(connectionProvider, ServiceFileUriQueryParamsProvider())
					val bestMatchUriProvider = BestMatchUriProvider(
						library,
						StoredFileUriProvider(
							selectedLibraryProvider,
							StoredFileAccess(this),
							arbitratorForOs),
						CachedAudioFileUriProvider(
							remoteFileUriProvider,
							CachedFilesProvider(this, cacheConfiguration)),
						MediaFileUriProvider(
							this,
							MediaQueryCursorProvider(this, cachedFileProperties),
							arbitratorForOs,
							selectedLibraryIdentifierProvider,
							false),
						remoteFileUriProvider)

					val promisedPreparationSourceProvider = playbackHandler.value.then { ph ->
						val playbackEngineBuilder = PreparedPlaybackQueueFeederBuilder(
							this,
							ph,
							Handler(mainLooper),
							MediaSourceProvider(
								library,
								HttpDataSourceFactoryProvider(this, connectionProvider, OkHttpFactory),
								simpleCache),
							bestMatchUriProvider
						)

						MaxFileVolumePreparationProvider(
							playbackEngineBuilder.build(library),
							MaxFileVolumeProvider(volumeLevelSettings, cachedSessionFilePropertiesProvider))
					}

					Promise
						.whenAll(promisedMediaBroadcaster, promisedMediaNotificationSetup.unitResponse())
						.eventually { promisedPreparationSourceProvider }
				}
			}
			.then { preparationSourceProvider ->
				PreparedPlaybackQueueResourceManagement(preparationSourceProvider, preparationSourceProvider)
					.also {
						playbackEngineCloseables.manage(it)
						playbackQueues = it
					}
			}
			.eventually { queues ->
				PlaylistPlaybackBootstrapper(playlistVolumeManager)
					.also {
						playbackEngineCloseables.manage(it)
						playlistPlaybackBootstrapper = it
					}
					.let { bootstrapper ->
						getNewNowPlayingRepository()
							.then { r ->
								r?.let {
									PlaybackEngine(
										queues,
										QueueProviders.providers(),
										r,
										bootstrapper
									)
								}
							}
				}
			}
			.eventually { engine ->
				engine
					?.also {
						playbackEngineCloseables.manage(engine)
						playbackState = AudioManagingPlaybackStateChanger(
							engine,
							AudioFocusManagement(audioManager),
							playlistVolumeManager
						).also(playbackEngineCloseables::manage)
					}
					?.setOnPlaybackStarted(::handlePlaybackStarted)
					?.setOnPlaybackPaused(::handlePlaybackPaused)
					?.setOnPlayingFileChanged(::changePositionedPlaybackFile)
					?.setOnPlaylistError(::uncaughtExceptionHandler)
					?.setOnPlaybackCompleted(::onPlaylistPlaybackComplete)
					?.setOnPlaylistReset(::broadcastResetPlaylist)
					?.also {
						playlistPosition = it
						playlistFiles = it
						playbackContinuity = it
					}
					?.restoreFromSavedState()
					?.then { file ->
						playbackBroadcaster.sendPlaybackBroadcast(PlaylistEvents.onPlaylistPause)
						broadcastChangedFile(
							PositionedFile(
								file.playlistPosition,
								file.serviceFile,
							)
						)
						trackPositionBroadcaster?.broadcastProgress(file)
						engine
					}
					.keepPromise()
			}
	}

	private fun handleBuildConnectionStatusChange(status: Int) {
		val notifyBuilder = NotificationCompat.Builder(this, playbackNotificationsConfiguration.notificationChannel)
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
		isMarkedForPlay = true
		playbackStartedBroadcaster.broadcastPlaybackStarted()
	}

	private fun handlePlaybackPaused() {
		isMarkedForPlay = false
		playbackBroadcaster.sendPlaybackBroadcast(PlaylistEvents.onPlaylistPause)

		filePositionSubscription?.dispose()
	}

	private fun uncaughtExceptionHandler(exception: Throwable?) {
		when (exception) {
			is PlaybackEngineInitializationException -> handlePlaybackEngineInitializationException(exception)
			is PreparationException -> handlePreparationException(exception)
			is IOException -> handleIoException(exception)
			is ExoPlaybackException -> handleExoPlaybackException(exception)
			is PlaybackException -> handlePlaybackException(exception)
			is TimeoutException -> handleTimeoutException(exception)
			else -> {
				logger.error("An unexpected error has occurred!", exception)
				UnexpectedExceptionToaster.announce(this, exception)
				stopSelf(startId)
			}
		}
	}

	private fun handlePlaybackEngineInitializationException(exception: PlaybackEngineInitializationException) {
		logger.error("There was an error initializing the playback engine", exception)
		stopSelf(startId)
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
				closeAndRestartPlaylistManager(exception)
			}
			is IOException -> handleIoException(cause)
			null -> logger.error("An unexpected playback exception occurred", exception)
			else -> uncaughtExceptionHandler(cause)
		}
	}

	private fun handleExoPlaybackException(exception: ExoPlaybackException) {
		logger.error("An ExoPlaybackException occurred")

		when (val cause = exception.cause) {
			is IllegalStateException -> {
				logger.error("The ExoPlayer player ended up in an illegal state, closing and restarting the player", cause)
				closeAndRestartPlaylistManager(exception)
				return
			}
			is NoSuchElementException -> {
				logger.error("The ExoPlayer player was unable to deque data, closing and restarting the player", cause)
				closeAndRestartPlaylistManager(exception)
				return
			}
			null -> stopSelf(startId)
			else -> uncaughtExceptionHandler(exception.cause)
		}
	}

	private fun handleIoException(exception: IOException?) {
		if (exception is InvalidResponseCodeException && exception.responseCode == 416) {
			logger.warn("Received an error code of " + exception.responseCode + ", will attempt restarting the player", exception)
			closeAndRestartPlaylistManager(exception)
			return
		}

		logger.error("An IO exception occurred during playback", exception)
		handleDisconnection()
	}

	private fun handleTimeoutException(exception: TimeoutException) {
		logger.warn("A timeout occurred during playback, will attempt restarting the player", exception)
		closeAndRestartPlaylistManager(exception)
	}

	private fun handleDisconnection() {
		if (disconnectionLatch.trigger()) {
			logger.error("Unable to re-connect after $numberOfDisconnects in less than $disconnectResetDuration, stopping the playback service.")
			stopSelf(startId)
			return
		}

		logger.warn("Number of disconnections has not surpassed $numberOfDisconnects in less than $disconnectResetDuration. Checking for disconnections.")
		pollSessionConnection(this, true)
			.then(connectionRegainedListener, onPollingCancelledListener)
	}

	private fun closeAndRestartPlaylistManager(error: Throwable) {
		if (errorLatch.trigger()) {
			logger.error("$numberOfErrors occurred within $errorLatchResetDuration, stopping the playback service. Last error: ${error.message}", error)
			stopSelf(startId)
			return
		}

		closeAndRestartPlaylistManager()
	}

	private fun closeAndRestartPlaylistManager() {
		try {
			playbackEngineCloseables.close()
		} catch (e: Exception) {
			uncaughtExceptionHandler(e)
			return
		}

		selectedLibraryProvider
			.browserLibrary
			.eventually { library ->
				library?.let(::initializePlaybackPlaylistStateManagerSerially).keepPromise()
			}
			.then { if (isMarkedForPlay) resumePlayback() }
			.excuse(unhandledRejectionHandler)
	}

	private fun changePositionedPlaybackFile(positionedPlayingFile: PositionedPlayingFile) {
		val playingFile = positionedPlayingFile.playingFile
		filePositionSubscription?.dispose()

		if (playingFile is EmptyPlaybackHandler) return

		broadcastChangedFile(positionedPlayingFile.asPositionedFile())
		selectedLibraryIdentifierProvider.selectedLibraryId.then {
			it?.also { l ->
				playbackBroadcaster.sendPlaybackBroadcast(
					PlaylistEvents.onPlaylistTrackStart,
					l,
					positionedPlayingFile.asPositionedFile()
				)
			}
		}
		val promisedPlayedFile = playingFile.promisePlayedFile()

		val localSubscription = trackPositionBroadcaster?.run {
			Observable.interval(1, TimeUnit.SECONDS, lazyObservationScheduler.value)
				.flatMapMaybe { promisedPlayedFile.progress.toMaybeObservable() }
				.distinctUntilChanged()
				.subscribe(observeUpdates(playingFile))
		}

		promisedPlayedFile.then {
			selectedLibraryIdentifierProvider.selectedLibraryId.then { l ->
				l?.also {
					playbackBroadcaster.sendPlaybackBroadcast(
						PlaylistEvents.onPlaylistTrackComplete,
						it,
						positionedPlayingFile.asPositionedFile()
					)
				}
				localSubscription?.dispose()
			}
		}

		filePositionSubscription = localSubscription

		if (!areListenersRegistered) registerListeners()
	}

	private fun broadcastResetPlaylist(positionedFile: PositionedFile) {
		selectedLibraryIdentifierProvider.selectedLibraryId.then { l ->
			l?.also {
				playbackBroadcaster.sendPlaybackBroadcast(
					PlaylistEvents.onPlaylistTrackChange,
					it,
					positionedFile
				)
			}
		}
	}

	private fun broadcastChangedFile(positionedFile: PositionedFile) {
		selectedLibraryIdentifierProvider.selectedLibraryId.then { l ->
			l?.also {
				playbackBroadcaster.sendPlaybackBroadcast(
					PlaylistEvents.onPlaylistTrackChange,
					it,
					positionedFile
				)
			}
		}
	}

	private fun onPlaylistPlaybackComplete() {
		playbackBroadcaster.sendPlaybackBroadcast(PlaylistEvents.onPlaylistStop)
		isMarkedForPlay = false
		stopSelf(startId)
	}

	override fun onDestroy() {
		isMarkedForPlay = false

		if (lazyNotificationController.isInitialized()) lazyNotificationController.value.removeAllNotifications()

		playbackEngineCloseables.close()

		if (areListenersRegistered) unregisterListeners()

		if (playbackThread.isInitialized()) playbackThread.value.then { it.quitSafely() }

		filePositionSubscription?.dispose()
		cache?.release()

		if (lazyMediaSessionService.isInitialized()) lazyMediaSessionService.value.then { unbindService(it.serviceConnection) }

		if (lazyObservationScheduler.isInitialized()) lazyObservationScheduler.value.shutdown()

		if (!localBroadcastManagerLazy.isInitialized()) return

		localBroadcastManagerLazy.value.unregisterReceiver(buildSessionReceiver)
		localBroadcastManagerLazy.value.unregisterReceiver(playbackHaltingEvent)

		remoteControlProxy?.also(localBroadcastManagerLazy.value::unregisterReceiver)
		playbackNotificationRouter?.also(localBroadcastManagerLazy.value::unregisterReceiver)
	}

	/* End Event Handlers */ /* Begin Binder Code */
	override fun onBind(intent: Intent) = binder

	/* End Binder Code */
	private object Action {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(Action::class.java) }

		/* String constant actions */
		val initialize by lazy { magicPropertyBuilder.buildProperty("initialize") }
		val launchMusicService by lazy { magicPropertyBuilder.buildProperty("launchMusicService") }
		val play by lazy { magicPropertyBuilder.buildProperty("play") }
		val pause by lazy { magicPropertyBuilder.buildProperty("pause") }
		val togglePlayPause by lazy { magicPropertyBuilder.buildProperty("togglePlayPause") }
		val repeating by lazy { magicPropertyBuilder.buildProperty("repeating") }
		val completing by lazy { magicPropertyBuilder.buildProperty("completing") }
		val previous by lazy { magicPropertyBuilder.buildProperty("previous") }
		val next by lazy { magicPropertyBuilder.buildProperty("then") }
		val seekTo by lazy { magicPropertyBuilder.buildProperty("seekTo") }
		val addFileToPlaylist by lazy { magicPropertyBuilder.buildProperty("addFileToPlaylist") }
		val removeFileAtPositionFromPlaylist by lazy { magicPropertyBuilder.buildProperty("removeFileAtPositionFromPlaylist") }
		val killMusicService by lazy { magicPropertyBuilder.buildProperty("killMusicService") }
		val validActions by lazy {
			setOf(
				initialize,
				launchMusicService,
				play,
				pause,
				togglePlayPause,
				previous,
				next,
				seekTo,
				repeating,
				completing,
				addFileToPlaylist,
				removeFileAtPositionFromPlaylist
			)
		}
		val playbackStartingActions by lazy { setOf(launchMusicService, play, togglePlayPause) }

		object Bag {
			private val magicPropertyBuilder by lazy { MagicPropertyBuilder(Bag::class.java) }

			/* Bag constants */
			val playlistPosition by lazy { magicPropertyBuilder.buildProperty("playlistPosition") }
			val filePlaylist by lazy { magicPropertyBuilder.buildProperty("filePlaylist") }
			val startPos by lazy { magicPropertyBuilder.buildProperty("startPos") }
			val filePosition by lazy { magicPropertyBuilder.buildProperty("filePosition") }
		}
	}

	private class UninitializedPlaybackEngineException : PlaybackEngineInitializationException("The playback engine did not properly initialize")
}
