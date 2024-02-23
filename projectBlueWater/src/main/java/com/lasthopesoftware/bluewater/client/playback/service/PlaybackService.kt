package com.lasthopesoftware.bluewater.client.playback.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
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
import androidx.lifecycle.LifecycleService
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlaybackException
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
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdatePlayStatsOnPlaybackCompletedReceiver
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.LibraryPlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.browsing.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionServiceProxy
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.playback.caching.AudioCacheConfiguration
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.DiskFileCacheSourceFactory
import com.lasthopesoftware.bluewater.client.playback.caching.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.client.playback.engine.AudioManagingPlaybackStateChanger
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackContinuity
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackState
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackStateForSystem
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaylistFiles
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaylistPosition
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
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotifyingLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.MediaStyleNotificationSetup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.NowPlayingNotificationBuilder
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.PlaybackStartingNotificationBuilder
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.InMemoryNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Action.Bag
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.receivers.AudioBecomingNoisyReceiver
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.client.servers.version.LibraryServerVersionProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.settings.volumeleveling.VolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.MediaSession.MediaSessionService
import com.lasthopesoftware.bluewater.shared.android.audiofocus.AudioFocusManagement
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.getIntent
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
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.toMaybeObservable
import com.lasthopesoftware.bluewater.shared.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay.Companion.delay
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.bluewater.shared.promises.getSafely
import com.lasthopesoftware.bluewater.shared.promises.toFuture
import com.lasthopesoftware.bluewater.shared.resilience.TimedCountdownLatch
import com.lasthopesoftware.resources.closables.PromisingCloseableManager
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.loopers.HandlerThreadCreator
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.schedulers.ExecutorScheduler
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.joda.time.Duration
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@UnstableApi open class PlaybackService :
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

		fun initialize(context: Context, libraryId: LibraryId) =
			context.safelyStartService(getNewSelfIntent(context, PlaybackEngineAction.Initialize(libraryId)))

		fun startPlaylist(context: Context, libraryId: LibraryId, filePos: Int, serializedFileList: String) {
			context.safelyStartServiceInForeground(
				getNewSelfIntent(
					context,
					PlaybackStartingAction.StartPlaylist(libraryId, filePos, serializedFileList)
				)
			)
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
					PlaybackStartingAction.Play(libraryId)
				)
			)

		fun pendingPlayingIntent(context: Context, libraryId: LibraryId): PendingIntent =
			getPendingIntent(context, PlaybackStartingAction.Play(libraryId))

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

		fun pendingKillService(context: Context): PendingIntent =
			getPendingIntent(context, PlaybackServiceAction.KillPlaybackService)

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

	private val promisingServiceCloseables = PromisingCloseableManager()
	private val applicationMessageBus by lazy { promisingServiceCloseables.manage(getApplicationMessageBus().getScopedMessageBus()) }
	private val lazyObservationScheduler = lazy { ExecutorScheduler(ThreadPools.compute, true, true) }
	private val binder by lazy { GenericBinder(this) }
	private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
	private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
	private val applicationSettings by lazy { getApplicationSettingsRepository() }
	private val libraryRepository by lazy { LibraryRepository(this) }
	private val playlistVolumeManager by lazy { PlaylistVolumeManager(1.0f) }
	private val volumeLevelSettings by lazy { VolumeLevelSettings(applicationSettings) }

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

	private val connectionNotificationsConfiguration by lazy {
		NotificationsConfiguration(
			activatedPlaybackNotificationChannelName,
			connectingNotificationId
		)
	}

	private val arbitratorForOs by lazy { OsPermissionsChecker(this) }

	private val lazyMediaSessionService by promisingServiceCloseables.manage(
		RetryOnRejectionLazyPromise {
			promiseBoundService<MediaSessionService>().then(promisingServiceCloseables::manage)
		}
	)

	private val promisedMediaSession by RetryOnRejectionLazyPromise {
		lazyMediaSessionService.then { c -> c.service.mediaSession }
	}

	private val mediaStyleNotificationSetup by RetryOnRejectionLazyPromise {
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

	private val playbackThread by promisingServiceCloseables.manage(RetryOnRejectionLazyPromise {
		HandlerThreadCreator
			.promiseNewHandlerThread(
				"Playback",
				Process.THREAD_PRIORITY_AUDIO
			)
			.then(promisingServiceCloseables::manage)
	})

	private val playbackStartingNotificationBuilder by lazy {
		PlaybackStartingNotificationBuilder(
			this,
			NotificationBuilderProducer(this),
			playbackNotificationsConfiguration,
			intentBuilder,
		)
	}

	private val connectionSessionManager by lazy { ConnectionSessionManager.get(this) }

	private val libraryConnectionProvider by lazy {
		NotifyingLibraryConnectionProvider(
			NotificationBuilderProducer(this),
			connectionSessionManager,
			connectionNotificationsConfiguration,
			notificationController,
			StringResources(this),
		)
	}

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
	private val notificationController by lazy {
		promisingServiceCloseables.manage(NotificationsController(this, notificationManager))
	}
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

	private val urlKeyProvider by lazy { UrlKeyProvider(libraryConnectionProvider) }

	private	val promisedMediaNotificationSetup by promisingServiceCloseables.manage(RetryOnRejectionLazyPromise {
		mediaStyleNotificationSetup.then { mediaStyleNotificationSetup ->
			val notificationBuilder = promisingServiceCloseables.manage(
					NowPlayingNotificationBuilder(
					this,
					mediaStyleNotificationSetup,
					urlKeyProvider,
					libraryFilePropertiesProvider,
					imageProvider
				)
			)

			promisingServiceCloseables.manage(
				PlaybackNotificationBroadcaster(
					nowPlayingRepository,
					applicationMessageBus,
					urlKeyProvider,
					notificationController,
					playbackNotificationsConfiguration,
					notificationBuilder,
					playbackStartingNotificationBuilder,
				)
			)
		}
	})

	private val trackPositionBroadcaster by lazy {
		TrackPositionBroadcaster(
			applicationMessageBus,
			libraryFilePropertiesProvider
		)
	}

	private val remoteFileUriProvider by lazy {
		RemoteFileUriProvider(libraryConnectionProvider, ServiceFileUriQueryParamsProvider)
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
		val storedFileAccess = StoredFileAccess(this)
		BestMatchUriProvider(
			libraryRepository,
			StoredFileUriProvider(
				storedFileAccess,
				arbitratorForOs,
				contentResolver),
			CachedAudioFileUriProvider(remoteFileUriProvider, audioCache),
			MediaFileUriProvider(
				MediaQueryCursorProvider(contentResolver, cachedFileProperties),
				arbitratorForOs,
				contentResolver
			),
			remoteFileUriProvider
		)
	}

	private val updatePlayStatsOnPlaybackCompletedReceiver by lazy {
		promisingServiceCloseables.manage(
			UpdatePlayStatsOnPlaybackCompletedReceiver(
				LibraryPlaystatsUpdateSelector(
					LibraryServerVersionProvider(libraryConnectionProvider),
					PlayedFilePlayStatsUpdater(libraryConnectionProvider),
					FilePropertiesPlayStatsUpdater(
						freshLibraryFileProperties,
						FilePropertyStorage(
							libraryConnectionProvider,
							ConnectionAuthenticationChecker(libraryConnectionProvider),
							revisionProvider,
							FilePropertyCache,
							applicationMessageBus
						),
					),
				),
				this,
			)
		)
	}

	private val playlistPlaybackBootstrapper by lazy { promisingServiceCloseables.manage(PlaylistPlaybackBootstrapper(playlistVolumeManager)) }

	private val promisedPlaybackServices = RetryOnRejectionLazyPromise {
		// Call the value to initialize the lazy promise
		val hotPromisedMediaNotificationSetup = promisedMediaNotificationSetup

		val httpDataSourceFactory = HttpDataSourceFactoryProvider(
			this,
			guaranteedLibraryConnectionProvider,
			OkHttpFactory
		)

		val promisingPlaybackEngineCloseables = promisingServiceCloseables.createNestedManager()

		val promisedEngine = playbackThread
			.then { h -> Handler(h.looper) }
			.then { ph ->
				MaxFileVolumePreparationProvider(
					ExoPlayerPlayableFilePreparationSourceProvider(
						this,
						ph,
						Handler(mainLooper),
						MediaSourceProvider(
							this,
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
				promisingPlaybackEngineCloseables.manage(
					PreparedPlaybackQueueResourceManagement(preparationSourceProvider, preparationSourceProvider)
				)
			}
			.then { preparedPlaybackQueueResourceManagement ->
				val engine = PlaybackEngine(
					preparedPlaybackQueueResourceManagement,
					QueueProviders.providers(),
					nowPlayingRepository,
					playlistPlaybackBootstrapper
				)

				engine
					.setOnPlaybackStarted(this)
					.setOnPlaybackPaused(this)
					.setOnPlaybackInterrupted(this)
					.setOnPlayingFileChanged(updatePlayStatsOnPlaybackCompletedReceiver)
					.setOnPlaylistError(::uncaughtExceptionHandler)
					.setOnPlaybackCompleted(this)
					.setOnPlaylistReset(this)
					.let(promisingPlaybackEngineCloseables::manage)
					.let {
						PlaybackServices(
							playbackState = promisingPlaybackEngineCloseables.manage(
								AudioManagingPlaybackStateChanger(
									engine,
									engine,
									AudioFocusManagement(audioManager),
									playlistVolumeManager
								)
							),
							systemPlaybackState = it,
							playlistFiles = it,
							playbackContinuity = it,
							playlistPosition = it,
						)
					}
			}

		hotPromisedMediaNotificationSetup
			.eventually { promisedEngine }
			.eventually(forward()) { e ->
				promisingPlaybackEngineCloseables
					.promiseClose()
					.then { throw e }
			}
	}

	private val unhandledRejectionHandler = ImmediateResponse<Throwable, Unit>(::uncaughtExceptionHandler)

	private var activeLibraryId: LibraryId? = null
	private var isMarkedForPlay = false
	private var areListenersRegistered = false
	private var filePositionSubscription: Disposable? = null
	private var wakeLock: WakeLock? = null
	private var startId = 0
	private var isDestroyed = false

	private fun stopNotificationIfNotPlaying() {
		if (!isMarkedForPlay) notificationController.removeNotification(playingNotificationId)
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

		fun handlePlaybackStartingAction(playbackStartingAction: PlaybackStartingAction): Promise<Unit> {
			stopNotificationIfNotPlaying()
			return when (playbackStartingAction) {
				is PlaybackStartingAction.Play -> resumePlayback(playbackStartingAction.libraryId)
				is PlaybackStartingAction.StartPlaylist -> {
					val (libraryId, playlistPosition, playlistString) = playbackStartingAction

					startNewPlaylist(libraryId, playlistString, playlistPosition)
				}
			}
		}

		fun handlePlaybackEngineAction(playbackEngineAction: PlaybackEngineAction): Promise<Unit> {
			return when (playbackEngineAction) {
				is PlaybackStartingAction -> handlePlaybackStartingAction(playbackEngineAction)
				is PlaybackEngineAction.TogglePlayPause -> {
					if (isMarkedForPlay) pausePlayback()
					else handlePlaybackStartingAction(PlaybackStartingAction.Play(playbackEngineAction.libraryId))
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
						.then { (l, p) -> broadcastChangedFile(l, p) }
				}
				is PlaybackEngineAction.Next -> {
					restorePlaybackServices(playbackEngineAction.libraryId)
						.eventually { it.playlistPosition.skipToNext() }
						.then { (l, p) -> broadcastChangedFile(l, p) }
				}
				is PlaybackEngineAction.Seek -> {
					val (libraryId, playlistPosition, filePosition) = playbackEngineAction
					restorePlaybackServices(libraryId)
						.eventually { it.playlistPosition.changePosition(playlistPosition, Duration.millis(filePosition.toLong())) }
						.then { (l, p) -> broadcastChangedFile(l, p) }
				}
				is PlaybackEngineAction.AddFileToPlaylist -> {
					val (libraryId, serviceFile) = playbackEngineAction
					restorePlaybackServices(libraryId)
						.eventually { it.playlistFiles.addFile(serviceFile) }
						.then { applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId)) }
						.eventually(LoopedInPromise.response({
							Toast.makeText(this, getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show()
						}, this))
				}
				is PlaybackEngineAction.RemoveFileAtPosition -> {
					val (libraryId, filePosition) = playbackEngineAction

					restorePlaybackServices(libraryId)
						.eventually { it.playlistFiles.removeFileAtPosition(filePosition) }
						.then {
							applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId))
						}
						.unitResponse()
				}
				is PlaybackEngineAction.MoveFile -> {
					val (libraryId, filePosition, newPosition) = playbackEngineAction

					restorePlaybackServices(libraryId)
						.eventually { it.playlistFiles.moveFile(filePosition, newPosition) }
						.then {
							applicationMessageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(libraryId))
						}
						.unitResponse()
				}
				is PlaybackEngineAction.ClearPlaylist -> {
					val (libraryId) = playbackEngineAction
					restorePlaybackServices(libraryId)
						.eventually { it.playlistFiles.clearPlaylist() }
						.then {
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
					{ throw TimeoutException("Timed out after $playbackStartTimeout") },
					{
						// avoid logging cancellation exceptions
						if (it !is CancellationException)
							throw it
					}
				)

			val promisedIntentHandling = handlePlaybackEngineAction(playbackEngineAction)
				.must {
					promisedTimeout.cancel()
				}

			Promise.whenAny(promisedIntentHandling, timeoutResponse).excuse(unhandledRejectionHandler)
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

		// Closing these resources before calling `stopSelf` let's us avoid application resources shutting down (such
		// as network connections) before we are ready for them to close.
		promisingServiceCloseables
			.promiseClose()
			.must { stopSelf(startId) }
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
		val playbackState = promisedPlaybackServices

		isMarkedForPlay = true
		return FileStringListUtilities
			.promiseParsedFileStringList(playlistString)
			.eventually { playlist ->
				if (!areListenersRegistered) registerListeners()

				playbackState.value.eventually {
					it.playbackState.startPlaylist(
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
		if (!areListenersRegistered) registerListeners()
		return restorePlaybackServices(libraryId).eventually { it.playbackState.resume() }
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

	private fun uncaughtExceptionHandler(exception: Throwable?) {
		fun handleDisconnection() {
			if (disconnectionLatch.trigger()) {
				logger.error("Unable to re-connect after $numberOfDisconnects in less than $disconnectResetDuration, stopping the playback service.")

				if (exception != null)
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
			if (exception is HttpDataSource.InvalidResponseCodeException && exception.responseCode == 416) {
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
			is CancellationException -> return
			is PlaybackEngineInitializationException -> handlePlaybackEngineInitializationException(exception)
			is PreparationException -> handlePreparationException(exception)
			is IOException -> handleIoException(exception)
			is ExoPlaybackException -> handleExoPlaybackException(exception)
			is PlaybackException -> handlePlaybackException(exception)
			is TimeoutException -> handleTimeoutException(exception)
			else -> {
				logger.error("An unexpected error has occurred!", exception)
				if (exception != null)
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
		promisedPlaybackServices
			.value
			.eventually { engine -> engine.systemPlaybackState.interrupt() }
			.eventually { if (isMarkedForPlay) resumePlayback(libraryId).unitResponse() else Unit.toPromise() }
			.excuse(unhandledRejectionHandler)
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
			.then {
				applicationMessageBus.sendMessage(
					LibraryPlaybackMessage.TrackCompleted(libraryId, positionedPlayingFile.serviceFile)
				)
			}
			.must { localSubscription.dispose() }

		filePositionSubscription = localSubscription

		if (!areListenersRegistered) registerListeners()
	}

	private fun broadcastChangedFile(libraryId: LibraryId, positionedFile: PositionedFile) {
		applicationMessageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(libraryId, positionedFile))
	}

	private fun haltService() {
		pausePlayback()
		stopSelf(startId)
	}

	override fun onDestroy() {
		isDestroyed = true
		isMarkedForPlay = false

		if (areListenersRegistered) unregisterListeners()

		filePositionSubscription?.dispose()

		if (lazyObservationScheduler.isInitialized()) lazyObservationScheduler.value.shutdown()

		try {
			pausePlayback()
				.inevitably { promisingServiceCloseables.promiseClose() }
				.toFuture()
				.getSafely()
		} catch (e: Throwable) {
			logger.error("An error occurred closing resources", e)
		}

		super.onDestroy()
	}

	private data class PlaybackServices(
		val playbackContinuity: ChangePlaybackContinuity,
		val playlistFiles: ChangePlaylistFiles,
		val playbackState: ChangePlaybackState,
		val systemPlaybackState: ChangePlaybackStateForSystem,
		val playlistPosition: ChangePlaylistPosition,
	)

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
	}

	private sealed interface PlaybackStartingAction : PlaybackEngineAction {
		@Parcelize
		data class StartPlaylist(override val libraryId: LibraryId, val playlistPosition: Int, val serializedPlaylist: String) : PlaybackStartingAction {
			@IgnoredOnParcel
			override val requestCode = 13
		}

		@Parcelize
		data class Play(override val libraryId: LibraryId) : PlaybackStartingAction {
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
