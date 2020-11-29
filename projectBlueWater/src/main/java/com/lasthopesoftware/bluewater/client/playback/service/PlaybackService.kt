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
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
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
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Action.Bag
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.*
import com.lasthopesoftware.bluewater.client.playback.service.exceptions.BreakConnection
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
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetAllStoredFilesInLibrary
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings
import com.lasthopesoftware.bluewater.settings.volumeleveling.VolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.GenericBinder
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder.Companion.buildMagicPropertyName
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.loopers.HandlerThreadCreator
import com.lasthopesoftware.resources.notifications.NoOpChannelActivator
import com.lasthopesoftware.resources.notifications.NotificationBuilderProducer
import com.lasthopesoftware.resources.notifications.control.ControlNotifications
import com.lasthopesoftware.resources.notifications.control.NotificationsController
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.VoidResponse
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.namehillsoftware.lazyj.CreateAndHold
import com.namehillsoftware.lazyj.Lazy
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.RxThreadFactory
import io.reactivex.internal.schedulers.SingleScheduler
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

class PlaybackService : Service(), OnAudioFocusChangeListener {

	/* End streamer intent helpers */
	private val notificationManagerLazy: CreateAndHold<NotificationManager?> = Lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
	private val audioManagerLazy: CreateAndHold<AudioManager> = Lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
	private val localBroadcastManagerLazy: CreateAndHold<LocalBroadcastManager> = Lazy { LocalBroadcastManager.getInstance(this) }
	private val remoteControlReceiver: CreateAndHold<ComponentName> = Lazy { ComponentName(packageName, RemoteControlReceiver::class.java.name) }
	private val remoteControlClient: CreateAndHold<RemoteControlClient> = object : AbstractSynchronousLazy<RemoteControlClient>() {
		override fun create(): RemoteControlClient {
			// build the PendingIntent for the remote control client
			val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
			mediaButtonIntent.component = remoteControlReceiver.getObject()
			val mediaPendingIntent = PendingIntent.getBroadcast(this@PlaybackService, 0, mediaButtonIntent, 0)
			// create and register the remote control client
			return RemoteControlClient(mediaPendingIntent)
		}
	}
	private val lazyMediaSession: CreateAndHold<MediaSessionCompat> = object : AbstractSynchronousLazy<MediaSessionCompat>() {
		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
		override fun create(): MediaSessionCompat {
			val newMediaSession = MediaSessionCompat(
				this@PlaybackService,
				mediaSessionTag)
			newMediaSession.setCallback(MediaSessionCallbackReceiver(this@PlaybackService))
			val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
			mediaButtonIntent.component = remoteControlReceiver.getObject()
			val mediaPendingIntent = PendingIntent.getBroadcast(this@PlaybackService, 0, mediaButtonIntent, 0)
			newMediaSession.setMediaButtonReceiver(mediaPendingIntent)
			return newMediaSession
		}
	}
	private val lazyPlaybackBroadcaster: CreateAndHold<IPlaybackBroadcaster> = Lazy { LocalPlaybackBroadcaster(localBroadcastManagerLazy.getObject()) }
	private val lazyChosenLibraryIdentifierProvider: CreateAndHold<ISelectedLibraryIdentifierProvider> = Lazy { SelectedBrowserLibraryIdentifierProvider(this) }
	private val lazyPlaybackStartedBroadcaster: CreateAndHold<PlaybackStartedBroadcaster> = Lazy { PlaybackStartedBroadcaster(localBroadcastManagerLazy.getObject()) }
	private val lazyLibraryRepository: CreateAndHold<LibraryRepository> = Lazy { LibraryRepository(this) }
	private val lazyPlaylistVolumeManager: CreateAndHold<PlaylistVolumeManager> = Lazy { PlaylistVolumeManager(1.0f) }
	private val lazyVolumeLevelSettings: CreateAndHold<IVolumeLevelSettings> = Lazy { VolumeLevelSettings(this) }
	private val lazyChannelConfiguration: CreateAndHold<ChannelConfiguration> = Lazy { SharedChannelProperties(this) }
	private val lazyPlaybackNotificationsConfiguration: CreateAndHold<NotificationsConfiguration> = object : AbstractSynchronousLazy<NotificationsConfiguration>() {
		override fun create(): NotificationsConfiguration {
			val notificationChannelActivator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManagerLazy.getObject()) else NoOpChannelActivator()
			val channelName = notificationChannelActivator.activateChannel(lazyChannelConfiguration.getObject())
			return NotificationsConfiguration(channelName, playingNotificationId)
		}
	}
	private val lazyMediaStyleNotificationSetup: CreateAndHold<MediaStyleNotificationSetup> = object : AbstractSynchronousLazy<MediaStyleNotificationSetup>() {
		override fun create(): MediaStyleNotificationSetup {
			return MediaStyleNotificationSetup(
				this@PlaybackService,
				NotificationBuilderProducer(this@PlaybackService),
				lazyPlaybackNotificationsConfiguration.getObject(),
				lazyMediaSession.getObject())
		}
	}
	private val lazyAllStoredFilesInLibrary: CreateAndHold<GetAllStoredFilesInLibrary> = Lazy { StoredFilesCollection(this) }
	private val extractorThread: CreateAndHold<Promise<HandlerThread>> = object : AbstractSynchronousLazy<Promise<HandlerThread>>() {
		override fun create(): Promise<HandlerThread> {
			return HandlerThreadCreator.promiseNewHandlerThread(
				"Media Extracting thread",
				Process.THREAD_PRIORITY_AUDIO)
		}
	}
	private val extractorHandler: CreateAndHold<Promise<Handler>> = object : AbstractSynchronousLazy<Promise<Handler>>() {
		override fun create(): Promise<Handler> {
			return extractorThread.getObject().then { h: HandlerThread -> Handler(h.looper) }
		}
	}
	private val lazyPlaybackStartingNotificationBuilder: CreateAndHold<PlaybackStartingNotificationBuilder> = object : AbstractSynchronousLazy<PlaybackStartingNotificationBuilder>() {
		override fun create(): PlaybackStartingNotificationBuilder {
			return PlaybackStartingNotificationBuilder(
				this@PlaybackService,
				NotificationBuilderProducer(this@PlaybackService),
				lazyPlaybackNotificationsConfiguration.getObject(),
				lazyMediaSession.getObject())
		}
	}
	private val lazySelectedLibraryProvider: CreateAndHold<ISelectedBrowserLibraryProvider> = object : AbstractSynchronousLazy<ISelectedBrowserLibraryProvider>() {
		override fun create(): ISelectedBrowserLibraryProvider {
			return SelectedBrowserLibraryProvider(
				SelectedBrowserLibraryIdentifierProvider(this@PlaybackService),
				LibraryRepository(this@PlaybackService))
		}
	}
	private val lazyFileProperties: CreateAndHold<ProvideLibraryFileProperties> = Lazy {
		FilePropertiesProvider(
			get(this),
			FilePropertyCache.getInstance())
	}
	private val lazyCachedFileProperties: CreateAndHold<CachedFilePropertiesProvider> = Lazy {
		CachedFilePropertiesProvider(
			get(this),
			FilePropertyCache.getInstance(),
			lazyFileProperties.getObject())
	}
	private val lazyAudioBecomingNoisyReceiver: CreateAndHold<AudioBecomingNoisyReceiver> = Lazy { AudioBecomingNoisyReceiver() }
	private val lazyNotificationController: CreateAndHold<ControlNotifications> = Lazy { NotificationsController(this, notificationManagerLazy.getObject()!!) }
	private val lazyDisconnectionTracker: CreateAndHold<BreakConnection> = Lazy { ConnectionCircuitTracker() }
	private var areListenersRegistered = false
	private var playbackEnginePromise: Promise<PlaybackEngine>? = null
	private var playbackEngine: PlaybackEngine? = null
	private var playbackQueues: PreparedPlaybackQueueResourceManagement? = null
	private var cachedSessionFilePropertiesProvider: CachedSessionFilePropertiesProvider? = null
	private var positionedPlayingFile: PositionedPlayingFile? = null
	var isPlaying = false
		private set
	private var filePositionSubscription: Disposable? = null
	private var playlistPlaybackBootstrapper: PlaylistPlaybackBootstrapper? = null
	private var remoteControlProxy: RemoteControlProxy? = null
	private var playbackNotificationRouter: PlaybackNotificationRouter? = null
	private var nowPlayingNotificationBuilder: NowPlayingNotificationBuilder? = null
	private var wakeLock: WakeLock? = null
	private var cache: SimpleCache? = null
	private var startId = 0

	private val connectionRegainedListener: CreateAndHold<ImmediateResponse<IConnectionProvider, Void>> = object : AbstractSynchronousLazy<ImmediateResponse<IConnectionProvider, Void>>() {
		override fun create(): ImmediateResponse<IConnectionProvider, Void> {
			return VoidResponse { playbackEngine?.resume() ?: stopSelf(startId) }
		}
	}
	private val onPollingCancelledListener: CreateAndHold<ImmediateResponse<Throwable, Void>> = object : AbstractSynchronousLazy<ImmediateResponse<Throwable, Void>>() {
		override fun create(): ImmediateResponse<Throwable, Void> {
			return VoidResponse { e: Throwable? ->
				if (e is CancellationException) {
					unregisterListeners()
					stopSelf(startId)
				}
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
	private val UnhandledRejectionHandler = ImmediateResponse<Throwable, Void?> { e: Throwable? ->
		uncaughtExceptionHandler(e)
		null
	}

	private fun stopNotificationIfNotPlaying() {
		if (!isPlaying) lazyNotificationController.getObject().removeNotification(playingNotificationId)
	}

	private fun notifyStartingService(): Promise<Unit> {
		return lazyPlaybackStartingNotificationBuilder.getObject()
			.promisePreparedPlaybackStartingNotification()
			.then { b ->
				lazyNotificationController.getObject().notifyForeground(
					b.build(), startingNotificationId)
			}
	}

	private fun registerListeners() {
		audioManagerLazy.getObject().requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
		wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, MediaPlayer::class.java.name)
		wakeLock?.acquire()
		registerRemoteClientControl()
		registerReceiver(
			lazyAudioBecomingNoisyReceiver.getObject(),
			IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
		areListenersRegistered = true
	}

	private fun registerRemoteClientControl() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			lazyMediaSession.getObject().isActive = true
			return
		}
		audioManagerLazy.getObject().registerMediaButtonEventReceiver(remoteControlReceiver.getObject())
		audioManagerLazy.getObject().registerRemoteControlClient(remoteControlClient.getObject())
	}

	private fun unregisterListeners() {
		audioManagerLazy.getObject().abandonAudioFocus(this)
		if (wakeLock != null) {
			if (wakeLock!!.isHeld) wakeLock!!.release()
			wakeLock = null
		}
		if (lazyAudioBecomingNoisyReceiver.isCreated) unregisterReceiver(lazyAudioBecomingNoisyReceiver.getObject())
		areListenersRegistered = false
	}

	/* Begin Event Handlers */
	override fun onCreate() {
		registerRemoteClientControl()
		localBroadcastManagerLazy.getObject()
			.registerReceiver(
				onPlaybackEngineChanged,
				IntentFilter(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeChanged))
		localBroadcastManagerLazy.getObject()
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
			actOnIntent(intent).excuse(UnhandledRejectionHandler)
			return START_NOT_STICKY
		}

		notifyStartingService()
			.eventually { lazySelectedLibraryProvider.getObject().browserLibrary }
			.eventually { initializePlaybackPlaylistStateManagerSerially(it) }
			.eventually { actOnIntent(intent) }
			.must { lazyNotificationController.getObject().removeNotification(startingNotificationId) }
			.excuse(UnhandledRejectionHandler)
		return START_NOT_STICKY
	}

	private fun actOnIntent(intent: Intent?): Promise<Unit> {
		if (intent == null) return Unit.toPromise()
		var action = intent.action ?: return Unit.toPromise()
		val playbackEngine = playbackEngine ?: return Unit.toPromise()
		if (action == Action.launchMusicService) {
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
				.then { localBroadcastManagerLazy.getObject().sendBroadcast(Intent(PlaylistEvents.onPlaylistChange)) }
		}

		if (action == Action.togglePlayPause) action = if (isPlaying) Action.pause else Action.play

		if (action == Action.play) {
			isPlaying = true
			return playbackEngine.resume().then {}
		}

		if (action == Action.pause) return pausePlayback(true)

		if (!Action.playbackStartingActions.contains(action)) stopNotificationIfNotPlaying()

		if (action == Action.repeating) {
			playbackEngine.playRepeatedly()
			return Unit.toPromise()
		}

		if (action == Action.completing) {
			playbackEngine.playToCompletion()
			return Unit.toPromise()
		}

		if (action == Action.seekTo) {
			val playlistPosition = intent.getIntExtra(Bag.playlistPosition, -1)
			if (playlistPosition < 0) return Unit.toPromise()
			val filePosition = intent.getIntExtra(Bag.startPos, -1)
			return if (filePosition < 0) Unit.toPromise() else playbackEngine
				.changePosition(playlistPosition, filePosition)
				.then { positionedFile -> broadcastChangedFile(positionedFile) }
		}

		if (action == Action.previous) {
			return playbackEngine
				.skipToPrevious()
				.then { positionedFile: PositionedFile -> broadcastChangedFile(positionedFile) }
		}
		if (action == Action.next) {
			return playbackEngine
				.skipToNext()
				.then { positionedFile: PositionedFile -> broadcastChangedFile(positionedFile) }
		}

		if (action == Action.addFileToPlaylist) {
			val fileKey = intent.getIntExtra(Bag.playlistPosition, -1)
			return if (fileKey < 0) Unit.toPromise() else playbackEngine
				.addFile(ServiceFile(fileKey))
				.then(VoidResponse { localBroadcastManagerLazy.getObject().sendBroadcast(Intent(PlaylistEvents.onPlaylistChange)) })
				.eventually(LoopedInPromise.response({
					Toast.makeText(this, getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show()
				}, this))
		}

		if (action == Action.removeFileAtPositionFromPlaylist) {
			val filePosition = intent.getIntExtra(Bag.filePosition, -1)
			return if (filePosition < -1) Unit.toPromise() else playbackEngine
				.removeFileAtPosition(filePosition)
				.then {
					localBroadcastManagerLazy.getObject().sendBroadcast(Intent(PlaylistEvents.onPlaylistChange))
					Unit
				}
		}

		return Unit.toPromise()
	}

	@Synchronized
	private fun initializePlaybackPlaylistStateManagerSerially(library: Library): Promise<PlaybackEngine> {
		return playbackEnginePromise?.eventually(
			{ initializePlaybackEngine(library) },
			{ initializePlaybackEngine(library) })
			?: initializePlaybackEngine(library).also { playbackEnginePromise = it }
	}

	private fun initializePlaybackEngine(library: Library): Promise<PlaybackEngine> {
		if (playbackEngine != null) playbackEngine!!.close()
		return sessionConnection.eventually { connectionProvider: IConnectionProvider? ->
			if (connectionProvider == null) throw PlaybackEngineInitializationException("connectionProvider was null!")
			extractorHandler.getObject().eventually { handler: Handler? ->
				cachedSessionFilePropertiesProvider = CachedSessionFilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance(),
					SessionFilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance()))
				if (remoteControlProxy != null) localBroadcastManagerLazy.getObject().unregisterReceiver(remoteControlProxy!!)
				val imageProvider = ImageProvider(
					StaticLibraryIdentifierProvider(lazyChosenLibraryIdentifierProvider.getObject()),
					MemoryCachedImageAccess.getInstance(this))
				remoteControlProxy = RemoteControlProxy(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) MediaSessionBroadcaster(
					this,
					cachedSessionFilePropertiesProvider,
					imageProvider,
					lazyMediaSession.getObject()) else RemoteControlClientBroadcaster(
					this,
					cachedSessionFilePropertiesProvider,
					imageProvider,
					remoteControlClient.getObject()))
				localBroadcastManagerLazy
					.getObject()
					.registerReceiver(
						remoteControlProxy!!,
						buildRemoteControlProxyIntentFilter(remoteControlProxy!!))
				if (playbackNotificationRouter != null) localBroadcastManagerLazy.getObject().unregisterReceiver(playbackNotificationRouter!!)
				if (nowPlayingNotificationBuilder != null) nowPlayingNotificationBuilder!!.close()
				playbackNotificationRouter = PlaybackNotificationRouter(PlaybackNotificationBroadcaster(
					lazyNotificationController.getObject(),
					lazyPlaybackNotificationsConfiguration.getObject(),
					NowPlayingNotificationBuilder(
						this,
						lazyMediaStyleNotificationSetup.getObject(),
						connectionProvider,
						cachedSessionFilePropertiesProvider,
						imageProvider).also {
						nowPlayingNotificationBuilder = it
					},
					lazyPlaybackStartingNotificationBuilder.getObject()))
				localBroadcastManagerLazy
					.getObject()
					.registerReceiver(
						playbackNotificationRouter!!,
						buildNotificationRouterIntentFilter(playbackNotificationRouter!!))
				if (playlistPlaybackBootstrapper != null) playlistPlaybackBootstrapper!!.close()
				playlistPlaybackBootstrapper = PlaylistPlaybackBootstrapper(
					lazyPlaylistVolumeManager.getObject())
				val storedFileAccess = StoredFileAccess(
					this,
					lazyAllStoredFilesInLibrary.getObject())
				val arbitratorForOs = ExternalStorageReadPermissionsArbitratorForOs(this)
				val cacheConfiguration = AudioCacheConfiguration(library)
				if (cache != null) cache!!.release()
				cache = SimpleCache(
					AndroidDiskCacheDirectoryProvider(this).getDiskCacheDirectory(cacheConfiguration),
					LeastRecentlyUsedCacheEvictor(cacheConfiguration.maxSize))
				val remoteFileUriProvider = RemoteFileUriProvider(
					connectionProvider,
					ServiceFileUriQueryParamsProvider())
				val bestMatchUriProvider = BestMatchUriProvider(
					library,
					StoredFileUriProvider(
						lazySelectedLibraryProvider.getObject(),
						storedFileAccess,
						arbitratorForOs),
					CachedAudioFileUriProvider(
						remoteFileUriProvider,
						CachedFilesProvider(this, AudioCacheConfiguration(library))),
					MediaFileUriProvider(
						this,
						MediaQueryCursorProvider(this, lazyCachedFileProperties.getObject()),
						arbitratorForOs,
						lazyChosenLibraryIdentifierProvider.getObject(),
						false),
					remoteFileUriProvider)
				val playbackEngineBuilder = PreparedPlaybackQueueFeederBuilder(
					this,
					handler!!,
					MediaSourceProvider(
						library,
						HttpDataSourceFactoryProvider(this, connectionProvider, OkHttpFactory.getInstance()),
						cache!!),
					bestMatchUriProvider)
				val preparationSourceProvider: IPlayableFilePreparationSourceProvider = MaxFileVolumePreparationProvider(
					playbackEngineBuilder.build(library),
					MaxFileVolumeProvider(
						lazyVolumeLevelSettings.getObject(),
						cachedSessionFilePropertiesProvider))
				if (playbackQueues != null) playbackQueues!!.close()
				playbackQueues = PreparedPlaybackQueueResourceManagement(preparationSourceProvider, preparationSourceProvider)
				createEngine(
					playbackQueues!!,
					QueueProviders.providers(),
					NowPlayingRepository(
						SpecificLibraryProvider(
							lazyChosenLibraryIdentifierProvider.getObject().selectedLibraryId!!,
							lazyLibraryRepository.getObject()),
						lazyLibraryRepository.getObject()),
					playlistPlaybackBootstrapper!!)
			}
		}
			.then { engine: PlaybackEngine ->
				playbackEngine = engine
				engine
					.setOnPlaybackStarted { handlePlaybackStarted() }
					.setOnPlayingFileChanged { positionedPlayingFile: PositionedPlayingFile -> changePositionedPlaybackFile(positionedPlayingFile) }
					.setOnPlaylistError { exception: Throwable? -> uncaughtExceptionHandler(exception) }
					.setOnPlaybackCompleted { onPlaylistPlaybackComplete() }
					.setOnPlaylistReset { positionedFile: PositionedFile -> broadcastResetPlaylist(positionedFile) }
				engine
			}
	}

	private val sessionConnection: Promise<IConnectionProvider>
		get() {
			localBroadcastManagerLazy.getObject()
				.registerReceiver(
					buildSessionReceiver,
					IntentFilter(SessionConnection.buildSessionBroadcast))
			return SessionConnection.getInstance(this).promiseSessionConnection().must {
				localBroadcastManagerLazy.getObject().unregisterReceiver(buildSessionReceiver)
				lazyNotificationController.getObject().removeNotification(connectingNotificationId)
			}
		}

	private fun handleBuildConnectionStatusChange(status: Int) {
		val notifyBuilder = NotificationCompat.Builder(this, lazyPlaybackNotificationsConfiguration.getObject().notificationChannel)
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
		lazyNotificationController.getObject().notifyForeground(
			buildFullNotification(notifyBuilder),
			connectingNotificationId)
	}

	private fun handlePlaybackStarted() {
		isPlaying = true
		lazyPlaybackStartedBroadcaster.getObject().broadcastPlaybackStarted()
	}

	private fun pausePlayback(isUserInterrupted: Boolean): Promise<Unit> {
		isPlaying = false
		if (isUserInterrupted && areListenersRegistered) unregisterListeners()
		return playbackEngine?.pause()
			?.then {
				positionedPlayingFile?.run {
					lazyPlaybackBroadcaster.getObject()
						.sendPlaybackBroadcast(
							PlaylistEvents.onPlaylistPause,
							lazyChosenLibraryIdentifierProvider.getObject().selectedLibraryId,
							this.asPositionedFile())
				}

				filePositionSubscription?.dispose()
			}
			?: Unit.toPromise()
	}

	private fun uncaughtExceptionHandler(exception: Throwable?) {
		if (exception is PlaybackEngineInitializationException) {
			handlePlaybackEngineInitializationException(exception)
			return
		}
		if (exception is PreparationException) {
			handlePreparationException(exception)
			return
		}
		if (exception is IOException) {
			handleIoException(exception as IOException?)
			return
		}
		if (exception is ExoPlaybackException) {
			handleExoPlaybackException(exception)
		}
		if (exception is PlaybackException) {
			handlePlaybackException(exception)
			return
		}
		logger.error("An unexpected error has occurred!", exception)
		lazyNotificationController.getObject().removeAllNotifications()
	}

	private fun handlePlaybackEngineInitializationException(exception: PlaybackEngineInitializationException) {
		logger.error("There was an error initializing the playback engine", exception)
		lazyNotificationController.getObject().removeAllNotifications()
	}

	private fun handlePreparationException(preparationException: PreparationException) {
		logger.error("An error occurred during file preparation for file " + preparationException.positionedFile.serviceFile, preparationException)
		uncaughtExceptionHandler(preparationException.cause)
	}

	private fun handlePlaybackException(exception: PlaybackException) {
		val cause = exception.cause
		if (cause is ExoPlaybackException) {
			handleExoPlaybackException(cause)
		}
		if (cause is IllegalStateException) {
			logger.error("The player ended up in an illegal state - closing and restarting the player", exception)
			closeAndRestartPlaylistManager()
			return
		}
		if (cause is IOException) {
			handleIoException(exception.cause as IOException?)
			return
		}
		if (cause != null) {
			uncaughtExceptionHandler(cause)
			return
		}
		logger.error("An unexpected playback exception occurred", exception)
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
		if (exception is InvalidResponseCodeException) {
			val i = exception
			if (i.responseCode == 416) {
				logger.warn("Received an error code of " + i.responseCode + ", will attempt restarting the player", i)
				closeAndRestartPlaylistManager()
				return
			}
		}
		logger.error("An IO exception occurred during playback", exception)
		handleDisconnection()
	}

	private fun handleDisconnection() {
		if (!lazyDisconnectionTracker.getObject().isConnectionPastThreshold()) return
		pollSessionConnection(this, true)
			.then(connectionRegainedListener.getObject(), onPollingCancelledListener.getObject())
	}

	private fun closeAndRestartPlaylistManager() {
		try {
			playbackEngine?.close()
		} catch (e: Exception) {
			uncaughtExceptionHandler(e)
			return
		}

		lazyLibraryRepository.getObject()
			.getLibrary(lazyChosenLibraryIdentifierProvider.getObject().selectedLibraryId!!)
			.eventually { library ->
				if (library != null)
					initializePlaybackPlaylistStateManagerSerially(library)
				else
					Promise.empty()
			}
			.then { if (isPlaying) playbackEngine?.resume() }
			.excuse(UnhandledRejectionHandler)
	}

	override fun onAudioFocusChange(focusChange: Int) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// resume playback
			if (lazyPlaylistVolumeManager.isCreated) lazyPlaylistVolumeManager.getObject().setVolume(1.0f)
			playbackEngine?.run { if (isPlaying) resume() }
			return
		}

		if (playbackEngine?.isPlaying != true) return

		when (focusChange) {
			AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
				// Lost focus but it will be regained... cannot release resources
				pausePlayback(false)
				return
			}
			AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->                // Lost focus for a short time, but it's ok to keep playing
				// at an attenuated level
				if (lazyPlaylistVolumeManager.isCreated) lazyPlaylistVolumeManager.getObject().setVolume(0.2f)
		}
	}

	private fun changePositionedPlaybackFile(positionedPlayingFile: PositionedPlayingFile) {
		this.positionedPlayingFile = positionedPlayingFile
		val playingFile = positionedPlayingFile.playingFile
		filePositionSubscription?.dispose()
		if (playingFile is EmptyPlaybackHandler) return
		broadcastChangedFile(positionedPlayingFile.asPositionedFile())
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistTrackStart, lazyChosenLibraryIdentifierProvider.getObject().selectedLibraryId, positionedPlayingFile.asPositionedFile())
		val promisedPlayedFile = playingFile.promisePlayedFile()
		filePositionSubscription = Observable.interval(1, TimeUnit.SECONDS, lazyObservationScheduler.getObject())
			.map { promisedPlayedFile.progress }
			.distinctUntilChanged()
			.subscribe(TrackPositionBroadcaster(
				localBroadcastManagerLazy.getObject(),
				playingFile))

		val localSubscription = filePositionSubscription
		promisedPlayedFile.then {
			lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistTrackComplete, lazyChosenLibraryIdentifierProvider.getObject().selectedLibraryId, positionedPlayingFile.asPositionedFile())
			localSubscription?.dispose()
		}
		if (!areListenersRegistered) registerListeners()
		registerRemoteClientControl()
	}

	private fun broadcastResetPlaylist(positionedFile: PositionedFile) {
		lazyPlaybackBroadcaster.getObject()
			.sendPlaybackBroadcast(
				PlaylistEvents.onPlaylistTrackChange,
				lazyChosenLibraryIdentifierProvider.getObject().selectedLibraryId,
				positionedFile)
	}

	private fun broadcastChangedFile(positionedFile: PositionedFile) {
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistTrackChange, lazyChosenLibraryIdentifierProvider.getObject().selectedLibraryId, positionedFile)
	}

	private fun onPlaylistPlaybackComplete() {
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistStop, lazyChosenLibraryIdentifierProvider.getObject().selectedLibraryId, positionedPlayingFile!!.asPositionedFile())
		killService(this)
	}

	override fun onDestroy() {
		if (lazyNotificationController.isCreated) lazyNotificationController.getObject().removeAllNotifications()
		if (playlistPlaybackBootstrapper != null) {
			try {
				playlistPlaybackBootstrapper!!.close()
			} catch (e: IOException) {
				logger.warn("There was an error closing the prepared playback bootstrapper", e)
			}
		}
		if (playbackEngine != null) {
			try {
				playbackEngine!!.close()
			} catch (e: Exception) {
				logger.warn("There was an error closing the playback engine", e)
			}
		}
		if (playbackQueues != null) {
			try {
				playbackQueues!!.close()
			} catch (e: Exception) {
				logger.warn("There was an error closing the prepared playback queue", e)
			}
		}
		if (areListenersRegistered) unregisterListeners()
		if (remoteControlReceiver.isCreated) audioManagerLazy.getObject().unregisterMediaButtonEventReceiver(remoteControlReceiver.getObject())
		if (remoteControlClient.isCreated) audioManagerLazy.getObject().unregisterRemoteControlClient(remoteControlClient.getObject())
		if (extractorThread.isCreated) extractorThread.getObject().then { obj: HandlerThread -> obj.quitSafely() }
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && lazyMediaSession.isCreated) {
			lazyMediaSession.getObject().isActive = false
			lazyMediaSession.getObject().release()
		}
		if (filePositionSubscription != null) filePositionSubscription!!.dispose()
		if (cache != null) cache!!.release()
		if (nowPlayingNotificationBuilder != null) nowPlayingNotificationBuilder!!.close()
		if (!localBroadcastManagerLazy.isCreated) return
		localBroadcastManagerLazy.getObject().unregisterReceiver(buildSessionReceiver)
		localBroadcastManagerLazy.getObject().unregisterReceiver(onLibraryChanged)
		localBroadcastManagerLazy.getObject().unregisterReceiver(onPlaybackEngineChanged)
		if (remoteControlProxy != null) localBroadcastManagerLazy.getObject().unregisterReceiver(remoteControlProxy!!)
		if (playbackNotificationRouter != null) localBroadcastManagerLazy.getObject().unregisterReceiver(playbackNotificationRouter!!)
	}

	/* End Event Handlers */ /* Begin Binder Code */
	override fun onBind(intent: Intent): IBinder? {
		return lazyBinder.getObject()
	}

	private val lazyBinder = Lazy<IBinder> { GenericBinder(this) }

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
		val validActions: Set<String?> = HashSet(Arrays.asList(launchMusicService,
			play,
			pause,
			togglePlayPause,
			previous,
			next,
			seekTo,
			repeating,
			completing,
			addFileToPlaylist,
			removeFileAtPositionFromPlaylist))
		val playbackStartingActions: Set<String?> = HashSet(Arrays.asList(
			launchMusicService,
			play,
			togglePlayPause))

		object Bag {
			private val magicPropertyBuilder = MagicPropertyBuilder(Bag::class.java)

			/* Bag constants */
			val playlistPosition = magicPropertyBuilder.buildProperty("playlistPosition")
			val filePlaylist = magicPropertyBuilder.buildProperty("filePlaylist")
			val startPos = magicPropertyBuilder.buildProperty("startPos")
			val filePosition = magicPropertyBuilder.buildProperty("filePosition")
		}
	}

	companion object {
		private val logger = LoggerFactory.getLogger(PlaybackService::class.java)
		private val mediaSessionTag = buildMagicPropertyName(PlaybackService::class.java, "mediaSessionTag")
		private const val playingNotificationId = 42
		private const val startingNotificationId = 53
		private const val connectingNotificationId = 70
		private val lazyObservationScheduler: CreateAndHold<Scheduler> = object : AbstractSynchronousLazy<Scheduler>() {
			override fun create(): Scheduler {
				return SingleScheduler(
					RxThreadFactory(
						"Playback Observation",
						Thread.MIN_PRIORITY,
						false
					))
			}
		}

		private fun getNewSelfIntent(context: Context, action: String): Intent {
			val newIntent = Intent(context, PlaybackService::class.java)
			newIntent.action = action
			return newIntent
		}

		fun launchMusicService(context: Context, serializedFileList: String?) {
			launchMusicService(context, 0, serializedFileList)
		}

		fun launchMusicService(context: Context, filePos: Int, serializedFileList: String?) {
			val svcIntent = getNewSelfIntent(context, Action.launchMusicService)
			svcIntent.putExtra(Bag.playlistPosition, filePos)
			svcIntent.putExtra(Bag.filePlaylist, serializedFileList)
			safelyStartService(context, svcIntent)
		}

		@JvmOverloads
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

		fun setRepeating(context: Context) {
			safelyStartService(context, getNewSelfIntent(context, Action.repeating))
		}

		fun setCompleting(context: Context) {
			safelyStartService(context, getNewSelfIntent(context, Action.completing))
		}

		fun addFileToPlaylist(context: Context, fileKey: Int) {
			val intent = getNewSelfIntent(context, Action.addFileToPlaylist)
			intent.putExtra(Bag.playlistPosition, fileKey)
			safelyStartService(context, intent)
		}

		fun removeFileAtPositionFromPlaylist(context: Context, filePosition: Int) {
			val intent = getNewSelfIntent(context, Action.removeFileAtPositionFromPlaylist)
			intent.putExtra(Bag.filePosition, filePosition)
			safelyStartService(context, intent)
		}

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
}
