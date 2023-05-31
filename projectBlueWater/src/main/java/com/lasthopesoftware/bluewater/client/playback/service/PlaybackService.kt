package com.lasthopesoftware.bluewater.client.playback.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.Process
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.files.cached.DiskFileCache
import com.lasthopesoftware.bluewater.client.browsing.files.cached.access.CachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileAccessTimeUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.DiskFileCacheStreamSupplier
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.browsing.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionServiceProxy
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.caching.AudioCacheConfiguration
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.DiskFileCacheSourceFactory
import com.lasthopesoftware.bluewater.client.playback.caching.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.client.playback.engine.AudioManagingPlaybackStateChanger
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackContinuity
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackState
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaylistFiles
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackCompleted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackInterrupted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackPaused
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackStarted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaylistReset
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.HttpDataSourceFactoryProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.MediaSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.QueueProviders
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparationProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.MediaStyleNotificationSetup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.NowPlayingNotificationBuilder
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.PlaybackStartingNotificationBuilder
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.InMemoryNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Action.Bag
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.receivers.AudioBecomingNoisyReceiver
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.settings.volumeleveling.VolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.MediaSession.MediaSessionController
import com.lasthopesoftware.bluewater.shared.android.MediaSession.MediaSessionService
import com.lasthopesoftware.bluewater.shared.android.audiofocus.AudioFocusManagement
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.makePendingIntentImmutable
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.NotificationBuilderProducer
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.SharedChannelProperties
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.android.services.GenericBinder
import com.lasthopesoftware.bluewater.shared.android.services.promiseBoundService
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.observables.toMaybeObservable
import com.lasthopesoftware.bluewater.shared.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay.Companion.delay
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.bluewater.shared.resilience.TimedCountdownLatch
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.loopers.HandlerThreadCreator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler
import org.joda.time.Duration
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

open class PlaybackService :
	LifecycleService(),
	OnPlaybackPaused,
	OnPlaybackInterrupted,
	OnPlaybackStarted,
	OnPlayingFileChanged,
	OnPlaybackCompleted,
	OnPlaylistReset
{
	companion object {
		private val logger by lazyLogger<PlaybackService>()

		private const val playingNotificationId = 42
		private const val connectingNotificationId = 70

		private const val numberOfDisconnects = 3
		private val disconnectResetDuration = Duration.standardMinutes(1)

		private const val numberOfErrors = 5
		private val errorLatchResetDuration = Duration.standardSeconds(3)

		private val playbackStartTimeout = Duration.standardMinutes(2)

		fun initialize(context: Context) =
			context.safelyStartService(getNewSelfIntent(context, Action.initialize))

		fun launchMusicService(context: Context, libraryId: LibraryId, serializedFileList: String) =
			launchMusicService(context, libraryId, 0, serializedFileList)

		fun launchMusicService(context: Context, libraryId: LibraryId, filePos: Int, serializedFileList: String) {
			val svcIntent = getNewSelfIntent(context, Action.launchMusicService)
			svcIntent.putExtra(Bag.libraryId, libraryId)
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

		fun moveFile(context: Context, filePosition: Int, newPosition: Int) {
			val intent = getNewSelfIntent(context, Action.moveFile).apply {
				putExtra(Bag.filePosition, filePosition)
				putExtra(Bag.newPosition, newPosition)
			}
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

		fun promiseIsMarkedForPlay(context: Context, libraryId: LibraryId): Promise<Boolean> =
			context.promiseBoundService<PlaybackService>()
				.then { h ->
					val isPlaying = h.service.run { activeLibraryId == libraryId && isMarkedForPlay }
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
				.setSmallIcon(R.drawable.now_playing_status_icon_white)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.build()
	}

	/* End streamer intent helpers */

	private val lifecycleCloseableManager = AutoCloseableManager()
	private val lazyObservationScheduler = lazy { ExecutorScheduler(ThreadPools.compute, true) }
	private val binder by lazy { GenericBinder(this) }
	private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
	private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
	private val applicationMessageBus by lazyScoped { getApplicationMessageBus().getScopedMessageBus() }
	private val applicationSettings by lazy { getApplicationSettingsRepository() }
	private val libraryRepository by lazy { LibraryRepository(this) }
	private val playlistVolumeManager by lazy { PlaylistVolumeManager(1.0f) }
	private val volumeLevelSettings by lazy { VolumeLevelSettings(applicationSettings) }

	private val channelConfiguration by lazy { SharedChannelProperties(this) }

	private val playbackNotificationsConfiguration by lazy {
		val notificationChannelActivator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManager) else NoOpChannelActivator()
		val channelName = notificationChannelActivator.activateChannel(channelConfiguration)
		NotificationsConfiguration(
			channelName,
			playingNotificationId
		)
	}

	private val arbitratorForOs by lazy { OsPermissionsChecker(this) }

	private val lazyMediaSessionService = lazy { promiseBoundService<MediaSessionService>() }

	private val promisedMediaSession by lazy { lazyMediaSessionService.value.then { c -> c.service.mediaSession } }

	private val mediaStyleNotificationSetup by lazy {
			promisedMediaSession.then { mediaSession ->
				MediaStyleNotificationSetup(
					this,
					NotificationBuilderProducer(this),
					playbackNotificationsConfiguration,
					mediaSession,
					intentBuilder,
				)
			}
		}

	private val playbackThread = RetryOnRejectionLazyPromise {
		HandlerThreadCreator.promiseNewHandlerThread(
			"Playback",
			Process.THREAD_PRIORITY_AUDIO)
	}

	private val playbackStartingNotificationBuilder by lazy {
		PlaybackStartingNotificationBuilder(
			this,
			NotificationBuilderProducer(this),
			playbackNotificationsConfiguration,
			intentBuilder,
		)
	}

	private val libraryConnectionProvider by lazy { ConnectionSessionManager.get(this) }

	private val guaranteedLibraryConnectionProvider by lazy { GuaranteedLibraryConnectionProvider(libraryConnectionProvider) }

	private val fileProperties by lazy {
		FilePropertiesProvider(
			libraryConnectionProvider,
			LibraryRevisionProvider(libraryConnectionProvider),
			FilePropertyCache
        )
	}

	private val cachedFileProperties by lazy {
		CachedFilePropertiesProvider(
			libraryConnectionProvider,
			FilePropertyCache,
			fileProperties)
	}

	private val diskFileAccessTimeUpdater by lazy { DiskFileAccessTimeUpdater(this) }
	private val audioDiskCacheDirectoryProvider by lazy { AndroidDiskCacheDirectoryProvider(this, AudioCacheConfiguration) }
	private val lazyAudioBecomingNoisyReceiver = lazy { AudioBecomingNoisyReceiver() }
	private val lazyNotificationController = lazy { NotificationsController(this, notificationManager) }
	private val disconnectionLatch by lazy { TimedCountdownLatch(numberOfDisconnects, disconnectResetDuration) }
	private val errorLatch by lazy { TimedCountdownLatch(numberOfErrors, errorLatchResetDuration) }
	private val intentBuilder by lazy { IntentBuilder(this) }

	private val pollConnectionServiceProxy by lazy { PollConnectionServiceProxy(this) }
	private val connectionRegainedListener by lazy { ImmediateResponse<IConnectionProvider, Unit> { closeAndRestartPlaylistManager() } }

	private val onPollingCancelledListener by lazy { ImmediateResponse<Throwable?, Unit> { e ->
			if (e is CancellationException) {
				unregisterListeners()
				stopSelf(startId)
			}
		}
	}

	private val playbackHaltingEvent = object : (ApplicationMessage) -> Unit {
		override fun invoke(message: ApplicationMessage) {
			pausePlayback()
			stopSelf(startId)
		}
	}

	private val nowPlayingRepository by lazy {
		NowPlayingRepository(
			getCachedSelectedLibraryIdProvider(),
			libraryRepository,
			libraryRepository,
			InMemoryNowPlayingState,
		)
	}

	private val revisionProvider by lazy { LibraryRevisionProvider(libraryConnectionProvider) }

	private val freshLibraryFileProperties by lazy {
		FilePropertiesProvider(
			libraryConnectionProvider,
			revisionProvider,
			FilePropertyCache,
		)
	}

	private val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			libraryConnectionProvider,
			FilePropertyCache,
			freshLibraryFileProperties,
		)
	}

	private	val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val promisedMediaBroadcaster by lazy {
		promisedMediaSession.then { mediaSession ->
			MediaSessionBroadcaster(
				nowPlayingRepository,
				libraryFilePropertiesProvider,
				imageProvider,
				MediaSessionController(mediaSession),
				applicationMessageBus
			).also(lifecycleCloseableManager::manage)
		}
	}

	private val urlKeyProvider by lazy { UrlKeyProvider(libraryConnectionProvider) }

	private	val promisedMediaNotificationSetup by lazy {
		mediaStyleNotificationSetup.then { mediaStyleNotificationSetup ->
			NowPlayingNotificationBuilder(
				this,
				mediaStyleNotificationSetup,
				urlKeyProvider,
				libraryFilePropertiesProvider,
				imageProvider
			)
				.also(lifecycleCloseableManager::manage)
				.let { builder ->
					PlaybackNotificationBroadcaster(
						nowPlayingRepository,
						applicationMessageBus,
						urlKeyProvider,
						lazyNotificationController.value,
						playbackNotificationsConfiguration,
						builder,
						playbackStartingNotificationBuilder,
					).also(lifecycleCloseableManager::manage)
				}
		}
	}

	private val trackPositionBroadcaster by lazy {
		TrackPositionBroadcaster(
			applicationMessageBus,
			libraryFilePropertiesProvider
		)
	}

	private val remoteFileUriProvider by lazy {
		RemoteFileUriProvider(
			libraryConnectionProvider,
			ServiceFileUriQueryParamsProvider
		)
	}

	private val audioCacheFilesProvider by lazy { CachedFilesProvider(this, AudioCacheConfiguration) }

	private val audioCacheStreamSupplier by lazy {
		DiskFileCacheStreamSupplier(
			audioDiskCacheDirectoryProvider,
			DiskFileCachePersistence(
				this,
				audioDiskCacheDirectoryProvider,
				AudioCacheConfiguration,
				audioCacheFilesProvider,
				diskFileAccessTimeUpdater
			),
			audioCacheFilesProvider
		)
	}

	private val bestMatchUriProvider by lazy {
		val audioCache = DiskFileCache(this, audioDiskCacheDirectoryProvider, AudioCacheConfiguration, audioCacheStreamSupplier, audioCacheFilesProvider, diskFileAccessTimeUpdater)
		BestMatchUriProvider(
			libraryRepository,
			StoredFileUriProvider(
				StoredFileAccess(this),
				arbitratorForOs),
			CachedAudioFileUriProvider(remoteFileUriProvider, audioCache),
			MediaFileUriProvider(
				MediaQueryCursorProvider(this, cachedFileProperties),
				arbitratorForOs,
				false,
				applicationMessageBus
			),
			remoteFileUriProvider
		)
	}

	private val playlistPlaybackBootstrapper by lazyScoped { PlaylistPlaybackBootstrapper(playlistVolumeManager) }

	private val promisedPlaybackEngine by RetryOnRejectionLazyPromise {
		val httpDataSourceFactory = HttpDataSourceFactoryProvider(
			this,
			guaranteedLibraryConnectionProvider,
			OkHttpFactory
		)

		playbackThread.value
			.then { h -> Handler(h.looper) }
			.then { ph ->
				MaxFileVolumePreparationProvider(
					ExoPlayerPlayableFilePreparationSourceProvider(
						this,
						ph,
						Handler(mainLooper),
						MediaSourceProvider(
							DiskFileCacheSourceFactory(
								httpDataSourceFactory,
								audioCacheStreamSupplier
							),
							guaranteedLibraryConnectionProvider,
						),
						bestMatchUriProvider
					),
					MaxFileVolumeProvider(volumeLevelSettings, libraryFilePropertiesProvider)
				)
			}
			.then { preparationSourceProvider ->
				PreparedPlaybackQueueResourceManagement(preparationSourceProvider, preparationSourceProvider)
					.also(lifecycleCloseableManager::manage)
			}
			.then { preparedPlaybackQueueResourceManagement ->
				val engine = PlaybackEngine(
					preparedPlaybackQueueResourceManagement,
					QueueProviders.providers(),
					nowPlayingRepository,
					playlistPlaybackBootstrapper
				)

				engine
					.also {
						lifecycleCloseableManager.manage(engine)
						playbackState = AudioManagingPlaybackStateChanger(
							engine,
							engine,
							AudioFocusManagement(audioManager),
							playlistVolumeManager
						).also(lifecycleCloseableManager::manage)
					}
					.setOnPlaybackStarted(this)
					.setOnPlaybackPaused(this)
					.setOnPlaybackInterrupted(this)
					.setOnPlayingFileChanged(this)
					.setOnPlaylistError(::uncaughtExceptionHandler)
					.setOnPlaybackCompleted(this)
					.setOnPlaylistReset(this)
					.also {
						playlistFiles = it
						playbackContinuity = it
					}
			}
	}

	private val unhandledRejectionHandler = ImmediateResponse<Throwable, Unit>(::uncaughtExceptionHandler)

	private var activeLibraryId: LibraryId? = null
	private var isMarkedForPlay = false
	private var areListenersRegistered = false
	private var playbackContinuity: ChangePlaybackContinuity? = null
	private var playlistFiles: ChangePlaylistFiles? = null
	private var playbackState: ChangePlaybackState? = null
	private var filePositionSubscription: Disposable? = null
	private var wakeLock: WakeLock? = null
	private var startId = 0
	private var isDestroyed = false

	private fun stopNotificationIfNotPlaying() {
		if (!isMarkedForPlay) lazyNotificationController.value.removeNotification(playingNotificationId)
	}

	private fun registerListeners() {
		wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, javaClass.name)
		wakeLock?.acquire()
		registerReceiver(lazyAudioBecomingNoisyReceiver.value, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
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
		super.onCreate()

		guardDestroyedService()

		applicationMessageBus.registerForClass(
			cls<SelectedConnectionSettingsChangeReceiver.SelectedConnectionSettingsUpdated>(),
			playbackHaltingEvent
		)
		applicationMessageBus.registerForClass(
			cls<BrowserLibrarySelection.LibraryChosenMessage>(),
			playbackHaltingEvent)

		applicationMessageBus.registerForClass(
			cls<PlaybackEngineTypeChangedBroadcaster.PlaybackEngineTypeChanged>(),
			playbackHaltingEvent)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)

		fun getLibraryId(intent: Intent) = intent.safelyGetParcelableExtra<LibraryId>(Bag.libraryId)

		fun actOnIntent(intent: Intent): Promise<Unit> {
			var action = intent.action ?: return Unit.toPromise()

			if (action == Action.togglePlayPause) action = if (isMarkedForPlay) Action.pause else Action.play
			if (!Action.playbackStartingActions.contains(action)) stopNotificationIfNotPlaying()
			when (action) {
				Action.play -> return resumePlayback(getLibraryId(intent) ?: return Unit.toPromise())
				Action.pause, Action.initialize -> return pausePlayback()
				Action.repeating -> return playbackContinuity?.playRepeatedly() ?: Unit.toPromise()
				Action.completing -> return playbackContinuity?.playToCompletion() ?: Unit.toPromise()
				Action.previous -> {
					val libraryId = getLibraryId(intent) ?: return Unit.toPromise()

					return restorePlaybackEngine(libraryId)
						.eventually { it.skipToPrevious() }
						.then { (l, p) -> broadcastChangedFile(l, p) }
				}
				Action.next -> {
					val libraryId = getLibraryId(intent) ?: return Unit.toPromise()

					return restorePlaybackEngine(libraryId)
						.eventually { it.skipToNext() }
						.then { (l, p) -> broadcastChangedFile(l, p) }
				}
				Action.launchMusicService -> {
					val libraryId = getLibraryId(intent) ?: return Unit.toPromise()

					val playlistPosition = intent.getIntExtra(Bag.playlistPosition, -1)
					if (playlistPosition < 0) return Unit.toPromise()

					val playlistString = intent.getStringExtra(Bag.filePlaylist) ?: return Unit.toPromise()

					return startNewPlaylist(libraryId, playlistString, playlistPosition)
				}
				Action.seekTo -> {
					val libraryId = getLibraryId(intent) ?: return Unit.toPromise()

					val playlistPosition = intent.getIntExtra(Bag.playlistPosition, -1)
					if (playlistPosition < 0) return Unit.toPromise()

					val filePosition = intent.getIntExtra(Bag.startPos, -1)
					if (filePosition < 0) return Unit.toPromise()
					return restorePlaybackEngine(libraryId)
						.eventually { it.changePosition(playlistPosition, Duration.millis(filePosition.toLong())) }
						.then { (l, p) -> broadcastChangedFile(l, p) }
				}
				Action.addFileToPlaylist -> {
					val libraryId = getLibraryId(intent) ?: return Unit.toPromise()

					val fileKey = intent.getIntExtra(Bag.playlistPosition, -1)
					return if (fileKey < 0) Unit.toPromise() else restorePlaybackEngine(libraryId)
						.eventually { it.addFile(ServiceFile(fileKey)) }
						.then { applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId)) }
						.eventually(LoopedInPromise.response({
							Toast.makeText(this, getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show()
						}, this))
				}
				Action.removeFileAtPositionFromPlaylist -> {
					val libraryId = getLibraryId(intent) ?: return Unit.toPromise()

					val filePosition = intent.getIntExtra(Bag.filePosition, -1)
					return if (filePosition < 0) Unit.toPromise() else restorePlaybackEngine(libraryId)
						.eventually { it.removeFileAtPosition(filePosition) }
						.then {
							applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId))
						}
						.unitResponse()
				}
				Action.moveFile -> {
					val libraryId = getLibraryId(intent) ?: return Unit.toPromise()

					val filePosition = intent.getIntExtra(Bag.filePosition, -1)
					val newPosition = intent.getIntExtra(Bag.newPosition, -1)
					return if (filePosition < 0 || newPosition < 0) Unit.toPromise()
					else  restorePlaybackEngine(libraryId)
						.eventually { it.moveFile(filePosition, newPosition) }
						.then {
							applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId))
						}
						.unitResponse()
				}
				else -> return Unit.toPromise()
			}
		}

		fun timedActOnIntent(intent: Intent) {
			logger.debug("initializeEngineAndActOnIntent({})", intent)

			val promisedTimeout = delay<Any?>(playbackStartTimeout)

			val promisedIntentHandling = actOnIntent(intent)
				.must {
					promisedTimeout.cancel()
				}

			val timeoutResponse =
				promisedTimeout.then(
					{ throw TimeoutException("Timed out after $playbackStartTimeout") },
					{
						// avoid logging cancellation exceptions
						if (it !is CancellationException)
							throw it
					}
				)
			Promise.whenAny(promisedIntentHandling, timeoutResponse).excuse(unhandledRejectionHandler)
		}

		guardDestroyedService()

		this.startId = startId
		if (intent?.action == null) {
			stopSelf(startId)
			return START_NOT_STICKY
		}

		val action = intent.action
		if (Action.killMusicService == action || !Action.validActions.contains(action)) {
			stopSelf(startId)
			return START_NOT_STICKY
		}

		timedActOnIntent(intent)

		return START_STICKY
	}

	override fun onPlaybackPaused() {
		isMarkedForPlay = false
		applicationMessageBus.sendMessage(PlaybackMessage.PlaybackPaused)

		filePositionSubscription?.dispose()
	}

	override fun onPlaybackInterrupted() {
		isMarkedForPlay = false
		applicationMessageBus.sendMessage(PlaybackMessage.PlaybackInterrupted)

		filePositionSubscription?.dispose()
	}

	override fun onPlaybackCompleted() {
		applicationMessageBus.sendMessage(PlaybackMessage.PlaybackStopped)
		isMarkedForPlay = false
		stopSelf(startId)
	}

	override fun onPlaybackStarted() {
		isMarkedForPlay = true
		applicationMessageBus.sendMessage(PlaybackMessage.PlaybackStarted)
	}

	override fun onPlaylistReset(libraryId: LibraryId, positionedFile: PositionedFile) {
		applicationMessageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(libraryId, positionedFile))
	}

	override fun onPlayingFileChanged(libraryId: LibraryId, positionedPlayingFile: PositionedPlayingFile) {
		changePositionedPlaybackFile(libraryId, positionedPlayingFile)
	}

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		return binder
	}

	private fun guardDestroyedService() {
		if (isDestroyed)
			throw UnsupportedOperationException("Cannot create PlaybackService after onDestroy is called")
	}

	private fun startNewPlaylist(libraryId: LibraryId, playlistString: String, playlistPosition: Int): Promise<Unit> {
		activeLibraryId = libraryId
		val playbackState = initializePlaybackEngine()

		isMarkedForPlay = true
		return FileStringListUtilities
			.promiseParsedFileStringList(playlistString)
			.eventually { playlist ->
				playbackState.eventually {
					it.startPlaylist(
						libraryId,
						playlist.toMutableList(),
						playlistPosition,
						Duration.ZERO
					)
				}
			}
			.then {
				startActivity(intentBuilder.buildNowPlayingIntent(libraryId))
				applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId))
			}
	}

	private fun resumePlayback(libraryId: LibraryId): Promise<Unit> {
		isMarkedForPlay = true
		return restorePlaybackEngine(libraryId).eventually { it.resume() }.then {
			if (!areListenersRegistered) registerListeners()
		}
	}

	private fun pausePlayback(): Promise<Unit> {
		isMarkedForPlay = false
		if (areListenersRegistered) unregisterListeners()
		return playbackState?.pause() ?: Unit.toPromise()
	}

	private fun restorePlaybackEngine(libraryId: LibraryId): Promise<PlaybackEngine> =
		initializePlaybackEngine()
			.eventually { engine ->
				engine
					.restoreFromSavedState(libraryId)
					.then { (libraryId, file) ->
						file?.also {
							broadcastChangedFile(libraryId, PositionedFile(it.playlistPosition, it.serviceFile))
							trackPositionBroadcaster.broadcastProgress(libraryId, it)
						}
						engine
					}
			}

	private fun initializePlaybackEngine(): Promise<PlaybackEngine> {
		return Promise
			.whenAll(promisedMediaBroadcaster.unitResponse(), promisedMediaNotificationSetup.unitResponse())
			.eventually { promisedPlaybackEngine }
	}

	private fun promiseLibraryConnection(libraryId: LibraryId) =
		libraryConnectionProvider.promiseLibraryConnection(libraryId).apply {
			updates(::handleBuildConnectionStatusChange)
		}

	private fun handleBuildConnectionStatusChange(status: BuildingConnectionStatus) {
		val notifyBuilder = NotificationCompat.Builder(this, playbackNotificationsConfiguration.notificationChannel)
		notifyBuilder
			.setOngoing(false)
			.setContentTitle(getText(R.string.title_svc_connecting_to_server))

		when (status) {
			BuildingConnectionStatus.GettingLibrary -> notifyBuilder.setContentText(getText(R.string.lbl_getting_library_details))
			BuildingConnectionStatus.GettingLibraryFailed -> {
				Toast.makeText(this, getText(R.string.lbl_please_connect_to_valid_server), Toast.LENGTH_SHORT).show()
				return
			}
			BuildingConnectionStatus.SendingWakeSignal -> notifyBuilder.setContentText(getString(R.string.sending_wake_signal))
			BuildingConnectionStatus.BuildingConnection -> notifyBuilder.setContentText(getText(R.string.lbl_connecting_to_server_library))
			BuildingConnectionStatus.BuildingConnectionFailed -> {
				Toast.makeText(this, getText(R.string.lbl_error_connecting_try_again), Toast.LENGTH_SHORT).show()
				return
			}
			BuildingConnectionStatus.BuildingConnectionComplete -> notifyBuilder.setContentText(getString(R.string.lbl_connected))
		}

		lazyNotificationController.value.notifyForeground(
			buildFullNotification(notifyBuilder),
			connectingNotificationId)
	}

	private fun uncaughtExceptionHandler(exception: Throwable?) {
		fun handleDisconnection() {
			if (disconnectionLatch.trigger()) {
				logger.error("Unable to re-connect after $numberOfDisconnects in less than $disconnectResetDuration, stopping the playback service.")
				UnexpectedExceptionToaster.announce(this, exception)
				stopSelf(startId)
				return
			}

			logger.warn("Number of disconnections has not surpassed $numberOfDisconnects in less than $disconnectResetDuration. Checking for disconnections.")

			activeLibraryId?.also {
				pollConnectionServiceProxy
					.pollConnection(it)
					.then(connectionRegainedListener, onPollingCancelledListener)
			}
		}

		fun handlePlaybackEngineInitializationException(exception: PlaybackEngineInitializationException) {
			logger.error("There was an error initializing the playback engine", exception)
			stopSelf(startId)
		}

		fun handlePreparationException(preparationException: PreparationException) {
			logger.error("An error occurred during file preparation for file " + preparationException.positionedFile.serviceFile, preparationException)
			uncaughtExceptionHandler(preparationException.cause)
		}

		fun handleExoPlaybackException(exception: ExoPlaybackException) {
			logger.error("An ExoPlaybackException occurred")

			when (val cause = exception.cause) {
				is IllegalStateException -> {
					logger.error("The ExoPlayer player ended up in an illegal state, closing and restarting the player", cause)
					closeAndRestartPlaylistManager(exception)
				}
				is NoSuchElementException -> {
					logger.error("The ExoPlayer player was unable to deque data, closing and restarting the player", cause)
					closeAndRestartPlaylistManager(exception)
				}
				null -> stopSelf(startId)
				else -> uncaughtExceptionHandler(exception.cause)
			}
		}

		fun handleIoException(exception: IOException?) {
			if (exception is InvalidResponseCodeException && exception.responseCode == 416) {
				logger.warn("Received an error code of " + exception.responseCode + ", will attempt restarting the player", exception)
				closeAndRestartPlaylistManager(exception)
				return
			}

			logger.error("An IO exception occurred during playback", exception)
			handleDisconnection()
		}

		fun handlePlaybackException(exception: PlaybackException) {
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

		fun handleTimeoutException(exception: TimeoutException) {
			logger.warn("A timeout occurred during playback, will attempt restarting the player", exception)
			closeAndRestartPlaylistManager(exception)
		}

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

	private fun closeAndRestartPlaylistManager(error: Throwable) {
		if (errorLatch.trigger()) {
			logger.error("$numberOfErrors occurred within $errorLatchResetDuration, stopping the playback service. Last error: ${error.message}", error)
			UnexpectedExceptionToaster.announce(this, error)
			stopSelf(startId)
			return
		}

		closeAndRestartPlaylistManager()
	}

	private fun closeAndRestartPlaylistManager() {
		val libraryId = activeLibraryId ?: return
		promisedPlaybackEngine
			.eventually { engine -> engine.interrupt() }
			.eventually { if (isMarkedForPlay) resumePlayback(libraryId).unitResponse() else Unit.toPromise() }
			.excuse(unhandledRejectionHandler)
	}

	private fun changePositionedPlaybackFile(libraryId: LibraryId, positionedPlayingFile: PositionedPlayingFile) {
		val playingFile = positionedPlayingFile.playingFile
		filePositionSubscription?.dispose()

		if (playingFile is EmptyPlaybackHandler) return

		broadcastChangedFile(libraryId, positionedPlayingFile.asPositionedFile())
		applicationMessageBus.sendMessage(LibraryPlaybackMessage.TrackStarted(libraryId, positionedPlayingFile.serviceFile))

		val promisedPlayedFile = playingFile.promisePlayedFile()
		val localSubscription = trackPositionBroadcaster.run {
			Observable.interval(1, TimeUnit.SECONDS, lazyObservationScheduler.value)
				.flatMapMaybe { promisedPlayedFile.progress.toMaybeObservable() }
				.distinctUntilChanged()
				.subscribe(observeUpdates(playingFile))
		}

		promisedPlayedFile
			.then {
				applicationMessageBus.sendMessage(
					LibraryPlaybackMessage.TrackCompleted(libraryId, positionedPlayingFile.serviceFile)
				)

				localSubscription?.dispose()
			}

		filePositionSubscription = localSubscription

		if (!areListenersRegistered) registerListeners()
	}

	private fun broadcastChangedFile(libraryId: LibraryId, positionedFile: PositionedFile) {
		applicationMessageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(libraryId, positionedFile))
	}

	override fun onDestroy() {
		isDestroyed = true
		isMarkedForPlay = false

		if (lazyNotificationController.isInitialized()) lazyNotificationController.value.removeAllNotifications()

		if (areListenersRegistered) unregisterListeners()

		filePositionSubscription?.dispose()

		if (lazyMediaSessionService.isInitialized()) lazyMediaSessionService.value.then { unbindService(it.serviceConnection) }

		if (lazyObservationScheduler.isInitialized()) lazyObservationScheduler.value.shutdown()

		pausePlayback()
			.inevitably {
				if (playbackThread.isInitializing()) playbackThread.value.then { it.quitSafely() }
				else Unit.toPromise()
			}
			.must { lifecycleCloseableManager.close() }

		super.onDestroy()
	}

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
		val next by lazy { magicPropertyBuilder.buildProperty("next") }
		val seekTo by lazy { magicPropertyBuilder.buildProperty("seekTo") }
		val addFileToPlaylist by lazy { magicPropertyBuilder.buildProperty("addFileToPlaylist") }
		val removeFileAtPositionFromPlaylist by lazy { magicPropertyBuilder.buildProperty("removeFileAtPositionFromPlaylist") }
		val moveFile by lazy { magicPropertyBuilder.buildProperty("moveFile") }
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
				removeFileAtPositionFromPlaylist,
				moveFile
			)
		}
		val playbackStartingActions by lazy { setOf(launchMusicService, play, togglePlayPause) }

		object Bag {
			private val magicPropertyBuilder by lazy { MagicPropertyBuilder(Bag::class.java) }

			/* Bag constants */
			val libraryId by lazy { magicPropertyBuilder.buildProperty("libraryId") }
			val playlistPosition by lazy { magicPropertyBuilder.buildProperty("playlistPosition") }
			val filePlaylist by lazy { magicPropertyBuilder.buildProperty("filePlaylist") }
			val startPos by lazy { magicPropertyBuilder.buildProperty("startPos") }
			val filePosition by lazy { magicPropertyBuilder.buildProperty("filePosition") }
			val newPosition by lazy { magicPropertyBuilder.buildProperty("newPosition") }
		}
	}

	private class UninitializedPlaybackEngineException : PlaybackEngineInitializationException("The playback engine did not properly initialize")
}
