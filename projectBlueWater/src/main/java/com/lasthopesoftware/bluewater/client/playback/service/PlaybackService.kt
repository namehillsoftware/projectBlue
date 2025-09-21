package com.lasthopesoftware.bluewater.client.playback.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Parcelable
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.Process
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ApplicationDependencies
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.intents.getIntent
import com.lasthopesoftware.bluewater.android.intents.makePendingIntentImmutable
import com.lasthopesoftware.bluewater.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.android.services.ControlService
import com.lasthopesoftware.bluewater.android.services.GenericBinder
import com.lasthopesoftware.bluewater.android.services.promiseBoundService
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LibraryFilePropertiesDependentsRegistry
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdatePlayStatsOnPlaybackCompletedReceiver
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.browsing.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionRegistry
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionServiceProxy
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.playback.caching.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.client.playback.engine.AudioManagingPlaybackStateChanger
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackContinuity
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackState
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackStateForSystem
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaylistFiles
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaylistPosition
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackCompleted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackInterrupted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackPaused
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackStarted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaylistError
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaylistReset
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.MediaSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.RemoteDataSourceFactoryProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.QueueProviders
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparationProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotifyingLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.MediaStyleNotificationSetup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.NowPlayingNotificationBuilder
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.PlaybackStartingNotificationBuilder
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Action.Bag
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.receivers.AudioBecomingNoisyReceiver
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.CompatibleMediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.bluewater.settings.volumeleveling.VolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.MediaSession.MediaSessionService
import com.lasthopesoftware.bluewater.shared.android.audiofocus.AudioFocusManagement
import com.lasthopesoftware.bluewater.shared.android.notifications.NoOpChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.NotificationBuilderProducer
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.SharedChannelProperties
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.toMaybeObservable
import com.lasthopesoftware.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.promises.PromiseDelay.Companion.delay
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.getSafely
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.closables.PromisingCloseableManager
import com.lasthopesoftware.resources.executors.HandlerExecutor
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.loopers.HandlerThreadCreator
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.schedulers.ExecutorScheduler
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.joda.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@UnstableApi open class PlaybackService :
	Service(),
	OnPlaybackPaused,
	OnPlaybackInterrupted,
	OnPlaybackStarted,
	OnPlayingFileChanged,
	OnPlaybackCompleted,
	OnPlaylistReset,
	ControlService,
	ResetPlaybackService,
	ProvideSelectedLibraryId
{
	companion object {
		private val logger by lazyLogger<PlaybackService>()

		private const val playingNotificationId = 42
		private const val connectingNotificationId = 70

		private val playbackStartTimeout = Duration.standardMinutes(2)

		fun initialize(context: Context, libraryId: LibraryId) =
			context.safelyStartService(getNewSelfIntent(context, PlaybackEngineAction.Initialize(libraryId)))

		fun startPlaylist(context: Context, libraryId: LibraryId, playlist: List<ServiceFile>, filePos: Int): Promise<Unit> {
			initialize(context, libraryId)
			return context.promiseBoundService<PlaybackService>()
				.eventually { h ->
					h.service
						.startNewPlaylist(libraryId, playlist, filePos)
						.must { _ -> h.close() }
				}
		}

		fun seekTo(context: Context, libraryId: LibraryId, filePos: Int, fileProgress: Int = 0) {
			context.safelyStartService(
				getNewSelfIntent(
					context,
					PlaybackEngineAction.Seek(libraryId, filePos, fileProgress)
				)
			)
		}

		fun play(context: Context, libraryId: LibraryId) =
			context.safelyStartServiceInForeground(
				getNewSelfIntent(
					context,
					PlaybackEngineAction.Play(libraryId)
				)
			)

		fun pendingPlayingIntent(context: Context, libraryId: LibraryId): PendingIntent =
			getPendingIntent(context, PlaybackEngineAction.Play(libraryId))

		@JvmStatic
		fun pause(context: Context) =
			context.safelyStartService(getNewSelfIntent(context, PlaybackServiceAction.Pause))

		@JvmStatic
		fun pendingPauseIntent(context: Context): PendingIntent =
			getPendingIntent(context, PlaybackServiceAction.Pause)

		fun togglePlayPause(context: Context, libraryId: LibraryId) =
			context.safelyStartService(
				getNewSelfIntent(
					context,
					PlaybackEngineAction.TogglePlayPause(libraryId)
				)
			)

		fun next(context: Context, libraryId: LibraryId) =
			context.safelyStartService(
				getNewSelfIntent(
					context,
					PlaybackEngineAction.Next(libraryId)
				)
			)

		fun pendingNextIntent(context: Context, libraryId: LibraryId): PendingIntent =
			getPendingIntent(context, PlaybackEngineAction.Next(libraryId))

		fun previous(context: Context, libraryId: LibraryId) =
			context.safelyStartService(
				getNewSelfIntent(
					context,
					PlaybackEngineAction.Previous(libraryId)
				)
			)

		fun pendingPreviousIntent(context: Context, libraryId: LibraryId): PendingIntent =
			getPendingIntent(context, PlaybackEngineAction.Previous(libraryId))

		fun setRepeating(context: Context, libraryId: LibraryId) =
			context.safelyStartService(getNewSelfIntent(context, PlaybackEngineAction.RepeatPlaylist(libraryId)))

		fun setCompleting(context: Context, libraryId: LibraryId) =
			context.safelyStartService(getNewSelfIntent(context, PlaybackEngineAction.CompletePlaylist(libraryId)))

		fun addFileToPlaylist(context: Context, libraryId: LibraryId, serviceFile: ServiceFile) {
			context.safelyStartService(
				getNewSelfIntent(
					context,
					PlaybackEngineAction.AddFileToPlaylist(libraryId, serviceFile)
				)
			)
		}

		fun addAfterNowPlayingFile(context: Context, libraryId: LibraryId, serviceFile: ServiceFile) {
			context.safelyStartService(
				getNewSelfIntent(
					context,
					PlaybackEngineAction.AddFileAfterNowPlaying(libraryId, serviceFile)
				)
			)
		}

		fun removeFileAtPositionFromPlaylist(context: Context, libraryId: LibraryId, filePosition: Int) {
			context.safelyStartService(
				getNewSelfIntent(
					context,
					PlaybackEngineAction.RemoveFileAtPosition(libraryId, filePosition)
				)
			)
		}

		fun moveFile(context: Context, libraryId: LibraryId, filePosition: Int, newPosition: Int) {
			context.safelyStartService(
				getNewSelfIntent(
					context,
					PlaybackEngineAction.MoveFile(libraryId, filePosition, newPosition)
				)
			)
		}

		fun clearPlaylist(context: Context, libraryId: LibraryId) {
			context.safelyStartService(getNewSelfIntent(context, PlaybackEngineAction.ClearPlaylist(libraryId)))
		}

		fun killService(context: Context) =
			context.safelyStartService(getNewSelfIntent(context, PlaybackServiceAction.KillPlaybackService))

		fun promiseIsMarkedForPlay(context: Context, libraryId: LibraryId): Promise<Boolean> =
			context.promiseBoundService<PlaybackService>()
				.then { h ->
					val isPlaying = h.service.run { activeLibraryId == libraryId && isMarkedForPlay }
					h.close()
					isPlaying
				}

		private fun getPendingIntent(context: Context, playbackServiceAction: PlaybackServiceAction) =
			PendingIntent.getService(
				context,
				playbackServiceAction.requestCode,
				getNewSelfIntent(context, playbackServiceAction),
				PendingIntent.FLAG_UPDATE_CURRENT.makePendingIntentImmutable()
			)

		private fun getNewSelfIntent(context: Context, playbackServiceAction: PlaybackServiceAction): Intent {
			val newIntent = context.getIntent<PlaybackService>()
			newIntent.action = Action.parsePlaybackServiceAction
			newIntent.putExtra(Bag.playbackServiceAction, playbackServiceAction)
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
	}

	/* End streamer intent helpers */

	private val playbackServiceDependencies by lazy { PlaybackServiceDependencies(this, applicationDependencies) }

	private val promisingServiceCloseables = PromisingCloseableManager()
	private val applicationMessageBus by lazy { promisingServiceCloseables.manage(getApplicationMessageBus().getScopedMessageBus()) }
	private val lazyObservationScheduler = lazy { ExecutorScheduler(ThreadPools.compute, true, true) }
	private val binder by lazy { GenericBinder(this) }
	private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
	private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
	private val playlistVolumeManager by lazy { PlaylistVolumeManager(1.0f) }

	private val channelConfiguration by lazy { SharedChannelProperties(this) }

	private val activatedPlaybackNotificationChannelName by lazy {
		val notificationChannelActivator =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManager)
			else NoOpChannelActivator
		notificationChannelActivator.activateChannel(channelConfiguration)
	}

	private val playbackNotificationsConfiguration by lazy {
		NotificationsConfiguration(
			activatedPlaybackNotificationChannelName,
			playingNotificationId
		)
	}

	private val arbitratorForOs by lazy { OsPermissionsChecker(this) }

	private val lazyMediaSessionService by promisingServiceCloseables.manage(
		RetryOnRejectionLazyPromise {
			promiseBoundService<MediaSessionService>()
				.cancelBackThen { boundService, _ ->
					promisingServiceCloseables.manage(boundService)
				}
		}
	)

	private val promisedMediaSession by promisingServiceCloseables.manage(RetryOnRejectionLazyPromise {
		lazyMediaSessionService.then { c -> c.service.mediaSession }
	})

	private val mediaStyleNotificationSetup by promisingServiceCloseables.manage(RetryOnRejectionLazyPromise {
		promisedMediaSession.then { mediaSession ->
			MediaStyleNotificationSetup(
				this,
				playbackServiceDependencies.notificationBuilderProducer,
				playbackNotificationsConfiguration,
				mediaSession,
				playbackServiceDependencies.intentBuilder,
			)
		}
	})

	private val mainLoopHandlerExecutor by lazy { HandlerExecutor(Handler(mainLooper)) }

	private val playbackThread by promisingServiceCloseables.manage(RetryOnRejectionLazyPromise {
		HandlerThreadCreator
			.promiseNewHandlerThread(
				"Playback",
				Process.THREAD_PRIORITY_AUDIO
			)
			.cancelBackThen { thread, _ -> promisingServiceCloseables.manage(thread) }
	})

	private val playbackStartingNotificationBuilder by lazy {
		PlaybackStartingNotificationBuilder(
			this,
			playbackServiceDependencies.notificationBuilderProducer,
			playbackNotificationsConfiguration,
			playbackServiceDependencies.intentBuilder,
		)
	}

	private val libraryConnectionProvider by lazy {
		playbackServiceDependencies.libraryConnectionProvider
	}

	private val libraryConnectionDependencies by lazy { LibraryConnectionRegistry(playbackServiceDependencies) }

	private val libraryFilePropertiesDependents by lazy {
		LibraryFilePropertiesDependentsRegistry(playbackServiceDependencies, libraryConnectionDependencies)
	}

	private val lazyAudioBecomingNoisyReceiver = lazy { AudioBecomingNoisyReceiver() }
	private val notificationController by lazy {
		promisingServiceCloseables.manage(NotificationsController(this, notificationManager))
	}

	private val pollConnectionServiceProxy by lazy { PollConnectionServiceProxy(this) }

	private val libraryFilePropertiesProvider by lazy {
		libraryConnectionDependencies.libraryFilePropertiesProvider
	}

	private val maxFileVolumeProvider by lazy {
		MaxFileVolumeProvider(
			VolumeLevelSettings(playbackServiceDependencies.applicationSettings),
			libraryFilePropertiesProvider
		)
	}

	private	val promisedMediaNotificationSetup by promisingServiceCloseables.manage(RetryOnRejectionLazyPromise {
		mediaStyleNotificationSetup.then { mediaStyleNotificationSetup ->
			with (libraryConnectionDependencies) {
				val notificationBuilder = promisingServiceCloseables.manage(
					NowPlayingNotificationBuilder(
						this@PlaybackService,
						mediaStyleNotificationSetup,
						urlKeyProvider,
						libraryFilePropertiesProvider,
						libraryFilePropertiesDependents.imageBytesProvider,
						playbackServiceDependencies.bitmapProducer,
					)
				)

				promisingServiceCloseables.manage(
					PlaybackNotificationBroadcaster(
						playbackServiceDependencies.nowPlayingState,
						applicationMessageBus,
						urlKeyProvider,
						notificationController,
						playbackNotificationsConfiguration,
						notificationBuilder,
						playbackStartingNotificationBuilder,
					)
				)
			}
		}
	})

	private val trackPositionBroadcaster by lazy {
		TrackPositionBroadcaster(
			applicationMessageBus,
			libraryFilePropertiesProvider
		)
	}

	private val bestMatchUriProvider by lazy {
		val remoteFileUriProvider = RemoteFileUriProvider(libraryConnectionProvider)
		val storedFileAccess = StoredFileAccess(this)
		BestMatchUriProvider(
			playbackServiceDependencies.libraryProvider,
			StoredFileUriProvider(
				storedFileAccess,
				arbitratorForOs,
				contentResolver),
			CachedAudioFileUriProvider(remoteFileUriProvider, playbackServiceDependencies.audioFileCache),
			CompatibleMediaFileUriProvider(
				libraryFilePropertiesProvider,
				arbitratorForOs,
				contentResolver
			),
			remoteFileUriProvider
		)
	}

	// Manage this resource separately, it needs to be cleaned up after everything else is cleaned up.
	private val updatePlayStatsOnPlaybackCompletedReceiver = lazy {
		UpdatePlayStatsOnPlaybackCompletedReceiver(
			PlayedFilePlayStatsUpdater(libraryConnectionProvider),
			this,
		)
	}

	private val mediaSourceProvider by lazy {
		MediaSourceProvider(
			this,
			RemoteDataSourceFactoryProvider(playbackServiceDependencies.guaranteedLibraryConnectionProvider),
		)
	}

	private val promisedPreparationSourceProvider by promisingServiceCloseables.manage(RetryOnRejectionLazyPromise {
		playbackThread
			.then { h -> Handler(h.looper) }
			.then { playbackHandler ->
				MaxFileVolumePreparationProvider(
					ExoPlayerPlayableFilePreparationSourceProvider(
						this,
						playbackHandler,
						mainLoopHandlerExecutor,
						mediaSourceProvider,
						bestMatchUriProvider
					),
					maxFileVolumeProvider
				)
			}
	})

	private val promisedPlaybackServices = promisingServiceCloseables.manage(RetryOnRejectionLazyPromise {
		// Call the value to initialize the lazy promise
		val hotPromisedMediaNotificationSetup = promisedMediaNotificationSetup

		val promisingPlaybackEngineCloseables = promisingServiceCloseables.createNestedManager()

		val promisedEngine = promisedPreparationSourceProvider
			.then { preparationSourceProvider ->
				promisingPlaybackEngineCloseables.manage(
					PreparedPlaybackQueueResourceManagement(preparationSourceProvider, preparationSourceProvider)
				)
			}
			.then { preparedPlaybackQueueResourceManagement ->
				val managedPlaylistPlayer = promisingServiceCloseables.manage(
					ManagedPlaylistPlayer(
						playlistVolumeManager,
						preparedPlaybackQueueResourceManagement,
						playbackServiceDependencies.nowPlayingStateMaintenance,
						QueueProviders.providers(),
					)
				)

				val engine = PlaybackEngine(
					preparedPlaybackQueueResourceManagement,
					QueueProviders.providers(),
					playbackServiceDependencies.nowPlayingStateMaintenance,
					managedPlaylistPlayer,
					managedPlaylistPlayer,
				)

				val playbackStateChanger = promisingPlaybackEngineCloseables.manage(
					AudioManagingPlaybackStateChanger(
						engine,
						engine,
						AudioFocusManagement(audioManager),
						playlistVolumeManager
					)
				)

				val errorHandler = PlaybackErrorHandler(
					this,
					pollConnectionServiceProxy,
					engine,
					playbackStateChanger,
					this,
					this,
					playbackServiceDependencies.exceptionAnnouncer,
				)

				engine
					.setOnPlaybackStarted(this)
					.setOnPlaybackPaused(this)
					.setOnPlaybackInterrupted(this)
					.setOnPlayingFileChanged(updatePlayStatsOnPlaybackCompletedReceiver.value)
					.setOnPlaylistError(errorHandler)
					.setOnPlaybackCompleted(this)
					.setOnPlaylistReset(this)
					.let(promisingPlaybackEngineCloseables::manage)
					.let {
						PlaybackServices(
							playbackState = playbackStateChanger,
							systemPlaybackState = it,
							playlistFiles = it,
							playbackContinuity = it,
							playlistPosition = it,
							errorHandler = errorHandler,
						)
					}
			}

		hotPromisedMediaNotificationSetup
			.eventually { promisedEngine }
			.eventually(forward()) { e ->
				promisingPlaybackEngineCloseables
					.promiseClose()
					.then { _ -> throw e }
			}
	})

	private var activeLibraryId: LibraryId? = null
	private var isMarkedForPlay = false
	private var areListenersRegistered = false
	private var filePositionSubscription: Disposable? = null
	private var wakeLock: WakeLock? = null
	private var startId = 0
	private var isDestroyed = false

	/* Begin Event Handlers */
	override fun onCreate() {
		super.onCreate()

		guardDestroyedService()

		// Message bus is service-scoped
		applicationMessageBus.registerReceiver { m: ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated ->
			if (m.libraryId == activeLibraryId)
				haltService()
		}

		applicationMessageBus.registerReceiver { m: BrowserLibrarySelection.LibraryChosenMessage ->
			if (m.chosenLibraryId != activeLibraryId)
				haltService()
		}

		applicationMessageBus.registerReceiver { _: PlaybackEngineTypeChangedBroadcaster.PlaybackEngineTypeChanged ->
			haltService()
		}
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)

		fun handlePlaybackEngineAction(playbackEngineAction: PlaybackEngineAction): Promise<Unit> {
			return when (playbackEngineAction) {
				is PlaybackEngineAction.Play -> resumePlayback(playbackEngineAction.libraryId)
				is PlaybackEngineAction.TogglePlayPause -> {
					if (isMarkedForPlay) pausePlayback()
					else resumePlayback(playbackEngineAction.libraryId)
				}
				is PlaybackEngineAction.Initialize -> restorePlaybackServices(playbackEngineAction.libraryId).unitResponse()
				is PlaybackEngineAction.RepeatPlaylist -> {
					restorePlaybackServices(playbackEngineAction.libraryId).eventually { it.playbackContinuity.playRepeatedly() }
				}
				is PlaybackEngineAction.CompletePlaylist -> {
					restorePlaybackServices(playbackEngineAction.libraryId).eventually { it.playbackContinuity.playToCompletion() }
				}
				is PlaybackEngineAction.Previous -> {
					restorePlaybackServices(playbackEngineAction.libraryId)
						.eventually { it.playlistPosition.skipToPrevious() }
						.then { pair -> pair?.let { (l, p) -> broadcastChangedFile(l, p) } }
				}
				is PlaybackEngineAction.Next -> {
					restorePlaybackServices(playbackEngineAction.libraryId)
						.eventually { it.playlistPosition.skipToNext() }
						.then { pair -> pair?.let { (l, p) -> broadcastChangedFile(l, p) } }
				}
				is PlaybackEngineAction.Seek -> {
					val (libraryId, playlistPosition, filePosition) = playbackEngineAction
					restorePlaybackServices(libraryId)
						.eventually { it.playlistPosition.changePosition(playlistPosition, Duration.millis(filePosition.toLong())) }
						.then { pair -> pair?.let { (l, p) -> broadcastChangedFile(l, p) } }
				}
				is PlaybackEngineAction.AddFileToPlaylist -> {
					val (libraryId, serviceFile) = playbackEngineAction
					restorePlaybackServices(libraryId)
						.eventually { it.playlistFiles.addFile(serviceFile) }
						.then { _ -> applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId)) }
						.eventually {
							mainLoopHandlerExecutor.preparePromise {
								Toast.makeText(
									this,
									getText(R.string.lbl_song_added_to_now_playing),
									Toast.LENGTH_SHORT
								).show()
							}
						}
				}
				is PlaybackEngineAction.AddFileAfterNowPlaying -> {
					val (libraryId, serviceFile) = playbackEngineAction
					restorePlaybackServices(libraryId)
						.eventually { it.playlistFiles.playFileNext(serviceFile) }
						.then { _ -> applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId)) }
						.eventually {
							mainLoopHandlerExecutor.preparePromise {
								Toast.makeText(
									this,
									getText(R.string.lbl_song_added_to_now_playing),
									Toast.LENGTH_SHORT
								).show()
							}
						}
				}
				is PlaybackEngineAction.RemoveFileAtPosition -> {
					val (libraryId, filePosition) = playbackEngineAction

					restorePlaybackServices(libraryId)
						.eventually { it.playlistFiles.removeFileAtPosition(filePosition) }
						.then { _ ->
							applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId))
						}
						.unitResponse()
				}
				is PlaybackEngineAction.MoveFile -> {
					val (libraryId, filePosition, newPosition) = playbackEngineAction

					restorePlaybackServices(libraryId)
						.eventually { it.playlistFiles.moveFile(filePosition, newPosition) }
						.then { _ ->
							applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId))
						}
						.unitResponse()
				}
				is PlaybackEngineAction.ClearPlaylist -> {
					val (libraryId) = playbackEngineAction
					restorePlaybackServices(libraryId)
						.eventually { it.playlistFiles.clearPlaylist() }
						.then { _ ->
							logger.debug("Playlist cleared")
							applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId))
						}
						.unitResponse()
				}
			}
		}

		fun processPlaybackEngineActionOnDeadline(playbackEngineAction: PlaybackEngineAction) {
			val promisedTimeout = delay<Any?>(playbackStartTimeout)

			val timeoutResponse =
				promisedTimeout.then(
					{ throw PlaybackStartingTimeoutException(playbackStartTimeout) },
					{ it ->
						// avoid logging cancellation exceptions
						if (it !is CancellationException)
							throw it
					}
				)

			val promisedIntentHandling = handlePlaybackEngineAction(playbackEngineAction)
				.must { _ -> promisedTimeout.cancel() }

			Promise
				.whenAny(promisedIntentHandling, timeoutResponse)
				.excuse { e ->
					// Kill the service if it doesn't start in time.
					if (e is PlaybackStartingTimeoutException) stop()
				}
		}

		fun processPlaybackServiceAction(playbackServiceAction: PlaybackServiceAction?): Int {
			logger.debug("processPlaybackServiceAction({})", playbackServiceAction)
			return when (playbackServiceAction) {
				null, is PlaybackServiceAction.KillPlaybackService -> {
					haltService()
					START_NOT_STICKY
				}

				is PlaybackServiceAction.Pause -> {
					pausePlayback()
					START_NOT_STICKY
				}

				is PlaybackEngineAction -> {
					val wasMarkedForPlay = isMarkedForPlay
					processPlaybackEngineActionOnDeadline(playbackServiceAction)
					if (!wasMarkedForPlay || playbackServiceAction !is PlaybackEngineAction.TogglePlayPause) START_STICKY
					else START_NOT_STICKY
				}
			}
		}

		guardDestroyedService()

		this.startId = startId

		val parcelableAction = intent?.safelyGetParcelableExtra<PlaybackServiceAction>(Bag.playbackServiceAction)
		if (parcelableAction == null) {
			stopSelf(startId)
			return START_NOT_STICKY
		}

		logger.debug("onStartCommand({}, {}, {})", intent, flags, startId)

		return processPlaybackServiceAction(parcelableAction)
	}

	override fun stop() {
		stopSelf(startId)
	}

	override fun promiseSelectedLibraryId(): Promise<LibraryId?> = activeLibraryId.toPromise()

	override fun resetPlaylistManager() {
		val libraryId = activeLibraryId ?: return
		promisedPlaybackServices
			.value
			.eventually { engine -> engine.systemPlaybackState.interrupt() }
			.eventually { if (isMarkedForPlay) resumePlayback(libraryId) else Unit.toPromise() }
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
		isMarkedForPlay = false
		applicationMessageBus.sendMessage(PlaybackMessage.PlaybackStopped)

		if (!updatePlayStatsOnPlaybackCompletedReceiver.isInitialized()) {
			if (areListenersRegistered) unregisterListeners()
			return
		}

		// Wait for any playback updates to finish before sending out the playback stopped message.
		updatePlayStatsOnPlaybackCompletedReceiver
			.value
			.promiseUpdatesFinish()
			.must { _ ->
				if (areListenersRegistered) unregisterListeners()
			}
	}

	override fun onPlaybackStarted() {
		isMarkedForPlay = true
		applicationMessageBus.sendMessage(PlaybackMessage.PlaybackStarted)
	}

	override fun onPlaylistReset(libraryId: LibraryId, positionedFile: PositionedFile) {
		applicationMessageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(libraryId, positionedFile))
	}

	override fun onPlayingFileChanged(libraryId: LibraryId, positionedPlayingFile: PositionedPlayingFile?) {
		changePositionedPlaybackFile(libraryId, positionedPlayingFile)
	}

	override fun onBind(intent: Intent): IBinder? = binder

	private fun startNewPlaylist(libraryId: LibraryId, playlist: List<ServiceFile>, playlistPosition: Int): Promise<Unit> {
		activeLibraryId = libraryId
		val playbackState = promisedPlaybackServices.value

		isMarkedForPlay = true
		applicationMessageBus.sendMessage(PlaybackMessage.PlaybackStarting)

		if (!areListenersRegistered) registerListeners()

		return playbackState.eventually {
			it.playbackState
				.startPlaylist(
					libraryId,
					playlist.toList(),
					playlistPosition
				)
				.then(
					{ _ ->
						startActivity(playbackServiceDependencies.intentBuilder.buildNowPlayingIntent(libraryId))
						applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId))
					},
					{ e -> it.errorHandler.onError(e) }
				)
		}
	}

	private fun guardDestroyedService() {
		if (isDestroyed)
			throw UnsupportedOperationException("Cannot create PlaybackService after onDestroy is called")
	}

	@SuppressLint("WakelockTimeout", "Unknown media playback time")
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

	private fun resumePlayback(libraryId: LibraryId): Promise<Unit> {
		isMarkedForPlay = true
		applicationMessageBus.sendMessage(PlaybackMessage.PlaybackStarting)

		if (!areListenersRegistered) registerListeners()
		return restorePlaybackServices(libraryId).eventually {
			it.playbackState.resume().then(forward()) { e -> it.errorHandler.onError(e) }
		}
	}

	private fun pausePlayback(): Promise<Unit> {
		isMarkedForPlay = false

		if (areListenersRegistered) unregisterListeners()

		return if (promisedPlaybackServices.isInitializing()) promisedPlaybackServices.value.eventually { it.playbackState.pause() }
		else Unit.toPromise()
	}

	private fun restorePlaybackServices(libraryId: LibraryId): Promise<PlaybackServices> {
		activeLibraryId = libraryId
		return promisedPlaybackServices
			.value
			.eventually { services ->
				services
					.systemPlaybackState
					.restoreFromSavedState(libraryId)
					.then { (libraryId, file) ->
						file?.also {
							broadcastChangedFile(libraryId, PositionedFile(it.playlistPosition, it.serviceFile))
							trackPositionBroadcaster.broadcastProgress(libraryId, it)
						}
						services
					}
			}
	}

	private fun changePositionedPlaybackFile(libraryId: LibraryId, positionedPlayingFile: PositionedPlayingFile?) {
		filePositionSubscription?.dispose()

		val playingFile = positionedPlayingFile?.playingFile ?: return

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
			.then { _ ->
				applicationMessageBus.sendMessage(
					LibraryPlaybackMessage.TrackCompleted(libraryId, positionedPlayingFile.serviceFile)
				)
			}
			.must { _ -> localSubscription.dispose() }

		filePositionSubscription = localSubscription

		if (!areListenersRegistered) registerListeners()
	}

	private fun broadcastChangedFile(libraryId: LibraryId, positionedFile: PositionedFile) {
		applicationMessageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(libraryId, positionedFile))
	}

	private fun haltService() = stopSelf()

	override fun onDestroy() {
		isDestroyed = true
		isMarkedForPlay = false

		if (areListenersRegistered) unregisterListeners()

		filePositionSubscription?.dispose()

		if (lazyObservationScheduler.isInitialized()) lazyObservationScheduler.value.shutdown()

		try {
			pausePlayback()
				.inevitably { promisingServiceCloseables.promiseClose() }
				.inevitably {
					// As mentioned at creation, clean this up last and separately from the other dependencies.
					updatePlayStatsOnPlaybackCompletedReceiver
						.takeIf { it.isInitialized() }
						?.value
						?.promiseClose()
						.keepPromise(Unit)
				}
				.toFuture()
				.getSafely()
		} catch (e: Throwable) {
			logger.error("An error occurred closing resources", e)
		}

		super.onDestroy()
	}

	private class PlaybackServiceDependencies(playbackService: PlaybackService, inner: ApplicationDependencies) : ApplicationDependencies by inner {
		private val connectionNotificationsConfiguration by lazy {
			NotificationsConfiguration(
				playbackService.activatedPlaybackNotificationChannelName,
				connectingNotificationId
			)
		}

		val notificationBuilderProducer by lazy { NotificationBuilderProducer(playbackService) }

		override val progressingLibraryConnectionProvider by lazy {
			NotifyingLibraryConnectionProvider(
				notificationBuilderProducer,
				inner.progressingLibraryConnectionProvider,
				connectionNotificationsConfiguration,
				playbackService.notificationController,
				inner.stringResources,
			)
		}

		override val libraryConnectionProvider: ProvideLibraryConnections
			get() = progressingLibraryConnectionProvider

		val guaranteedLibraryConnectionProvider by lazy { GuaranteedLibraryConnectionProvider(libraryConnectionProvider) }
	}

	private data class PlaybackServices(
		val playbackContinuity: ChangePlaybackContinuity,
		val playlistFiles: ChangePlaylistFiles,
		val playbackState: ChangePlaybackState,
		val systemPlaybackState: ChangePlaybackStateForSystem,
		val playlistPosition: ChangePlaylistPosition,
		val errorHandler: OnPlaylistError,
	)

	private class PlaybackStartingTimeoutException(duration: Duration) : TimeoutException("Timed out after $duration")

	/* End Binder Code */

	private sealed interface PlaybackServiceAction : Parcelable {

		val requestCode: Int

		@Parcelize
		data object KillPlaybackService : PlaybackServiceAction {
			@IgnoredOnParcel
			override val requestCode = 0
		}

		@Parcelize
		data object Pause : PlaybackServiceAction {
			@IgnoredOnParcel
			override val requestCode = 1
		}
	}

	private sealed interface PlaybackEngineAction : PlaybackServiceAction {

		val libraryId: LibraryId

		@Parcelize
		data class Initialize(override val libraryId: LibraryId) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 2
		}

		@Parcelize
		data class RepeatPlaylist(override val libraryId: LibraryId) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 3
		}

		@Parcelize
		data class CompletePlaylist(override val libraryId: LibraryId) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 4
		}

		@Parcelize
		data class TogglePlayPause(override val libraryId: LibraryId) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 5
		}

		@Parcelize
		data class Previous(override val libraryId: LibraryId) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 6
		}

		@Parcelize
		data class Next(override val libraryId: LibraryId) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 7
		}

		@Parcelize
		data class Seek(override val libraryId: LibraryId, val playlistPosition: Int, val filePosition: Int) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 8
		}

		@Parcelize
		data class AddFileToPlaylist(override val libraryId: LibraryId, val serviceFile: ServiceFile) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 9
		}

		@Parcelize
		data class RemoveFileAtPosition(override val libraryId: LibraryId, val position: Int) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 10
		}

		@Parcelize
		data class MoveFile(override val libraryId: LibraryId, val from: Int, val to: Int) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 11
		}

		@Parcelize
		data class ClearPlaylist(override val libraryId: LibraryId): PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 12
		}

		@Parcelize
		data class AddFileAfterNowPlaying(override val libraryId: LibraryId, val serviceFile: ServiceFile) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 13
		}

		@Parcelize
		data class Play(override val libraryId: LibraryId) : PlaybackEngineAction {
			@IgnoredOnParcel
			override val requestCode = 14
		}
	}

	object Action {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(Action::class.java) }

		/* String constant actions */
		val parsePlaybackServiceAction by lazy { magicPropertyBuilder.buildProperty("parsePlaybackServiceAction") }

		object Bag {
			private val magicPropertyBuilder by lazy { MagicPropertyBuilder(Bag::class.java) }

			/* Bag constants */
			val playbackServiceAction by lazy { magicPropertyBuilder.buildProperty("playbackServiceAction") }
		}
	}
}
