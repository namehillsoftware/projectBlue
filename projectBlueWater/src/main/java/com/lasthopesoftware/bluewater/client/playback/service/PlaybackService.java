package com.lasthopesoftware.bluewater.client.playback.service;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection.BuildingSessionConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.MediaPlayerPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.queues.QueueProviders;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.IPlaybackBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.LocalPlaybackBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaybackStartedBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.RemoteControlReceiver;
import com.lasthopesoftware.bluewater.client.playback.state.PlaylistManager;
import com.lasthopesoftware.bluewater.client.playback.state.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.settings.volumeleveling.IVolumeLevelSettings;
import com.lasthopesoftware.bluewater.client.settings.volumeleveling.VolumeLevelSettings;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.lazyj.AbstractSynchronousLazy;
import com.vedsoft.lazyj.ILazy;
import com.vedsoft.lazyj.Lazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class PlaybackService extends Service implements OnAudioFocusChangeListener {
	private static final Logger logger = LoggerFactory.getLogger(PlaybackService.class);

	private static Intent getNewSelfIntent(final Context context, String action) {
		final Intent newIntent = new Intent(context, PlaybackService.class);
		newIntent.setAction(action);
		return newIntent;
	}

	public static void launchMusicService(final Context context, String serializedFileList) {
		launchMusicService(context, 0, serializedFileList);
	}

	public static void launchMusicService(final Context context, int filePos, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.launchMusicService);
		svcIntent.putExtra(Action.Bag.playlistPosition, filePos);
		svcIntent.putExtra(Action.Bag.filePlaylist, serializedFileList);
		context.startService(svcIntent);
	}

	public static void seekTo(final Context context, int filePos) {
		final Intent svcIntent = getNewSelfIntent(context, Action.seekTo);
		svcIntent.putExtra(Action.Bag.playlistPosition, filePos);
		context.startService(svcIntent);
	}

	public static void seekTo(final Context context, int filePos, int fileProgress) {
		final Intent svcIntent = getNewSelfIntent(context, Action.seekTo);
		svcIntent.putExtra(Action.Bag.playlistPosition, filePos);
		svcIntent.putExtra(Action.Bag.startPos, fileProgress);
		context.startService(svcIntent);
	}

	public static void play(final Context context) {
		context.startService(getNewSelfIntent(context, Action.play));
	}

	public static void pause(final Context context) {
		context.startService(getNewSelfIntent(context, Action.pause));
	}

	public static void togglePlayPause(final Context context) {
		context.startService(getNewSelfIntent(context, Action.togglePlayPause));
	}

	public static void next(final Context context) {
		context.startService(getNewSelfIntent(context, Action.next));
	}

	public static void previous(final Context context) {
		context.startService(getNewSelfIntent(context, Action.previous));
	}

	public static void setRepeating(final Context context) {
		context.startService(getNewSelfIntent(context, Action.repeating));
	}

	public static void setCompleting(final Context context) {
		context.startService(getNewSelfIntent(context, Action.completing));
	}

	public static void addFileToPlaylist(final Context context, int fileKey) {
		final Intent intent = getNewSelfIntent(context, Action.addFileToPlaylist);
		intent.putExtra(Action.Bag.playlistPosition, fileKey);
		context.startService(intent);
	}

	public static void removeFileAtPositionFromPlaylist(final Context context, int filePosition) {
		final Intent intent = getNewSelfIntent(context, Action.removeFileAtPositionFromPlaylist);
		intent.putExtra(Action.Bag.filePosition, filePosition);
		context.startService(intent);
	}

	/* End streamer intent helpers */
	
	/* Miscellaneous programming related string constants */
	private static final String PEBBLE_NOTIFY_INTENT = "com.getpebble.action.NOW_PLAYING";
	private static final String WIFI_LOCK_SVC_NAME =  "project_blue_water_svc_lock";

	private static final int notificationId = 42;
	private static int startId;

	private static final int maxErrors = 3;
	private static final int errorCountResetDuration = 1000;

	private WifiLock wifiLock = null;
	private final ILazy<NotificationManager> notificationManagerLazy = new Lazy<>(() -> (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE));
	private final ILazy<AudioManager> audioManagerLazy = new Lazy<>(() -> (AudioManager)getSystemService(Context.AUDIO_SERVICE));
	private final ILazy<LocalBroadcastManager> localBroadcastManagerLazy = new Lazy<>(() -> LocalBroadcastManager.getInstance(this));
	private final ILazy<ComponentName> remoteControlReceiver = new Lazy<>(() -> new ComponentName(getPackageName(), RemoteControlReceiver.class.getName()));
	private final ILazy<RemoteControlClient> remoteControlClient = new AbstractSynchronousLazy<RemoteControlClient>() {
		@Override
		protected RemoteControlClient initialize() throws Exception {
			// build the PendingIntent for the remote control client
			final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			mediaButtonIntent.setComponent(remoteControlReceiver.getObject());
			final PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(PlaybackService.this, 0, mediaButtonIntent, 0);
			// create and register the remote control client
			final RemoteControlClient remoteControlClient = new RemoteControlClient(mediaPendingIntent);
			remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			remoteControlClient.setTransportControlFlags(
				RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
					RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
					RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
					RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
					RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
					RemoteControlClient.FLAG_KEY_MEDIA_STOP);

			return remoteControlClient;
		}
	};
	private final ILazy<IPlaybackBroadcaster> lazyPlaybackBroadcaster = new Lazy<>(() -> new LocalPlaybackBroadcaster(this));
	private final ILazy<ISelectedLibraryIdentifierProvider> lazyChosenLibraryIdentifierProvider = new Lazy<>(() -> new SelectedBrowserLibraryIdentifierProvider(this));
	private final ILazy<PlaybackStartedBroadcaster> lazyPlaybackStartedBroadcaster = new Lazy<>(() -> new PlaybackStartedBroadcaster(lazyChosenLibraryIdentifierProvider.getObject(), lazyPlaybackBroadcaster.getObject()));
	private final ILazy<LibraryRepository> lazyLibraryRepository = new Lazy<>(() -> new LibraryRepository(this));
	private final ILazy<PlaylistVolumeManager> lazyPlaylistVolumeManager = new Lazy<>(() -> new PlaylistVolumeManager(1.0f));
	private final ILazy<IVolumeLevelSettings> lazyVolumeLevelSettings = new Lazy<>(() -> new VolumeLevelSettings(this));

	private Bitmap remoteClientBitmap = null;
	private int numberOfErrors = 0;
	private long lastErrorTime = 0;

	private boolean areListenersRegistered = false;
	private boolean isNotificationForeground = false;

	private PlaylistManager playlistManager;
	private CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private PositionedPlaybackFile positionedPlaybackFile;
	private boolean isPlaying;
	private Disposable playbackFileChangedSubscription;
	private Disposable filePositionSubscription;
	private Disposable playbackFileChangesConnection;
	private PlaylistPlaybackBootstrapper playlistPlaybackBootstrapper;

	private final AbstractSynchronousLazy<Runnable> connectionRegainedListener = new AbstractSynchronousLazy<Runnable>() {
		@Override
		protected final Runnable initialize() throws Exception {
			return () -> {
				if (playlistManager == null) {
					stopSelf(startId);
					return;
				}

				playlistManager.resume().next(observable -> observePlaybackFileChanges(observable));
			};
		}
	};

	private final AbstractSynchronousLazy<Runnable> onPollingCancelledListener = new AbstractSynchronousLazy<Runnable>() {
		@Override
		protected final Runnable initialize() throws Exception {
			return () -> {
				unregisterListeners();
				stopSelf(startId);
			};
		}
	};

	private final BroadcastReceiver onLibraryChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			pausePlayback(true);

			final int chosenLibrary = intent.getIntExtra(LibrarySession.chosenLibraryInt, -1);
			if (chosenLibrary < 0) return;

			try {
				playlistManager.close();
			} catch (Exception e) {
				logger.error("There was an error closing the playbackPlaylistStateManager", e);
			}

			playlistManager = null;
			cachedFilePropertiesProvider = null;
		}
	};

	private final CarelessOneParameterFunction<Throwable, Void> UnhandledRejectionHandler = runCarelessly(this::uncaughtExceptionHandler);

	public boolean isPlaying() {
		return isPlaying;
	}

	private void notifyForeground(Builder notificationBuilder) {
		notificationBuilder.setSmallIcon(R.drawable.clearstream_logo_dark);
		notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		final Notification notification = notificationBuilder.build();

		if (!isNotificationForeground) {
			startForeground(notificationId, notification);
			isNotificationForeground = true;
			return;
		}
		
		notificationManagerLazy.getObject().notify(notificationId, notification);
	}
	
	private void stopNotification() {
		stopForeground(true);
		isNotificationForeground = false;
		notificationManagerLazy.getObject().cancel(notificationId);
	}

	private void notifyStartingService() {
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setOngoing(true);
		builder.setContentTitle(String.format(getString(R.string.lbl_starting_service), getString(R.string.app_name)));

		notifyForeground(builder);
	}
	
	private void registerListeners() {
		audioManagerLazy.getObject().requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
				
		wifiLock = ((WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_SVC_NAME);
        wifiLock.acquire();
		
        registerRemoteClientControl();
        
		areListenersRegistered = true;
	}
	
	private void registerRemoteClientControl() {
		audioManagerLazy.getObject().registerMediaButtonEventReceiver(remoteControlReceiver.getObject());
		audioManagerLazy.getObject().registerRemoteControlClient(remoteControlClient.getObject());
	}
	
	private void unregisterListeners() {
		audioManagerLazy.getObject().abandonAudioFocus(this);
		
		// release the wifilock if we still have it
		if (wifiLock != null) {
			if (wifiLock.isHeld()) wifiLock.release();
			wifiLock = null;
		}
		final PollConnection pollConnection = PollConnection.Instance.get(this);
		if (connectionRegainedListener.isInitialized())
			pollConnection.removeOnConnectionRegainedListener(connectionRegainedListener.getObject());
		if (onPollingCancelledListener.isInitialized())
			pollConnection.removeOnPollingCancelledListener(onPollingCancelledListener.getObject());
		
		areListenersRegistered = false;
	}
	
	/* Begin Event Handlers */

	@Override
	public final void onCreate() {
		registerRemoteClientControl();
	}

	@Override
	public final int onStartCommand(final Intent intent, int flags, int startId) {
		// Should be modified to save its state locally in the future.
		PlaybackService.startId = startId;

		if (!Action.validActions.contains(intent.getAction())) {
			stopSelf(startId);
			return START_NOT_STICKY;
		}

		if ((playlistManager == null || !playlistManager.isPlaying()) && Action.playbackStartingActions.contains(intent.getAction()))
			notifyStartingService();
		
		if (SessionConnection.isBuilt()) {
			if (playlistManager != null) {
				actOnIntent(intent);
				return START_NOT_STICKY;
			}

			lazyLibraryRepository.getObject()
				.getLibrary(lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId())
				.next(this::initializePlaybackPlaylistStateManager)
				.next(runCarelessly(m -> actOnIntent(intent)))
				.error(UnhandledRejectionHandler);

			return START_NOT_STICKY;
		}

		// TODO this should probably be its own service soon
		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

		final BroadcastReceiver buildSessionReceiver  = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final int buildStatus = intent.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1);
				handleBuildConnectionStatusChange(buildStatus, intent);

				if (BuildingSessionConnectionStatus.completeConditions.contains(buildStatus))
					localBroadcastManager.unregisterReceiver(this);
			}
		};

		localBroadcastManager.registerReceiver(buildSessionReceiver, new IntentFilter(SessionConnection.buildSessionBroadcast));

		handleBuildConnectionStatusChange(SessionConnection.build(this), intent);

		return START_NOT_STICKY;
	}
	
	private void handleBuildConnectionStatusChange(final int status, final Intent intentToRun) {
		final Builder notifyBuilder = new Builder(this);
		notifyBuilder.setContentTitle(getText(R.string.title_svc_connecting_to_server));
		switch (status) {
		case BuildingSessionConnectionStatus.GettingLibrary:
			notifyBuilder.setContentText(getText(R.string.lbl_getting_library_details));
			break;
		case BuildingSessionConnectionStatus.GettingLibraryFailed:
			Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_please_connect_to_valid_server), Toast.LENGTH_SHORT).show();
			stopSelf(startId);
			return;
		case BuildingSessionConnectionStatus.BuildingConnection:
			notifyBuilder.setContentText(getText(R.string.lbl_connecting_to_server_library));
			break;
		case BuildingSessionConnectionStatus.BuildingConnectionFailed:
			Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_error_connecting_try_again), Toast.LENGTH_SHORT).show();
			stopSelf(startId);
			return;
		case BuildingSessionConnectionStatus.GettingView:
			notifyBuilder.setContentText(getText(R.string.lbl_getting_library_views));
			break;
		case BuildingSessionConnectionStatus.GettingViewFailed:
			Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_library_no_views), Toast.LENGTH_SHORT).show();
			stopSelf(startId);
			return;
		case BuildingSessionConnectionStatus.BuildingSessionComplete:
			stopNotification();

			lazyLibraryRepository.getObject()
				.getLibrary(lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId())
				.next(this::initializePlaybackPlaylistStateManager)
				.next(runCarelessly(m -> actOnIntent(intentToRun)))
				.error(UnhandledRejectionHandler);

			return;
		}
		notifyForeground(notifyBuilder);
	}

	private PlaylistManager initializePlaybackPlaylistStateManager(Library library) throws Exception {
		if (playlistManager != null)
			playlistManager.close();

		final SpecificLibraryProvider libraryProvider =
			new SpecificLibraryProvider(
				lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(),
				lazyLibraryRepository.getObject());

		final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();

		cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance(), new FilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance()));

		if (playlistPlaybackBootstrapper != null)
			playlistPlaybackBootstrapper.close();

		playlistPlaybackBootstrapper = new PlaylistPlaybackBootstrapper(
			lazyPlaylistVolumeManager.getObject(),
			new PlaybackHandlerVolumeControllerFactory(
				new MaxFileVolumeProvider(lazyVolumeLevelSettings.getObject(), cachedFilePropertiesProvider)));

		playlistManager =
			new PlaylistManager(
				new MediaPlayerPlaybackPreparerProvider(this, new BestMatchUriProvider(this, connectionProvider, library), library),
				QueueProviders.providers(),
				new NowPlayingRepository(libraryProvider, lazyLibraryRepository.getObject()),
				playlistPlaybackBootstrapper);

		return playlistManager;
	}
	
	private void actOnIntent(final Intent intent) {
		if (intent == null) {
			pausePlayback(true);
			return;
		}
		
		String action = intent.getAction();
		if (action == null) return;

		if (action.equals(Action.repeating)) {
			playlistManager.playRepeatedly();
			return;
		}

		if (action.equals(Action.completing)) {
			playlistManager.playToCompletion();
			return;
		}

		if (action.equals(Action.launchMusicService)) {
			final int playlistPosition = intent.getIntExtra(Action.Bag.playlistPosition, -1);
			if (playlistPosition < 0) return;

			FileStringListUtilities
				.promiseParsedFileStringList(intent.getStringExtra(Action.Bag.filePlaylist))
				.then(playlist -> playlistManager.startPlaylist(playlist, playlistPosition, 0))
				.next(this::observePlaybackFileChanges)
				.next(lazyPlaybackStartedBroadcaster.getObject())
				.next(this::handlePlaybackStarted)
				.error(UnhandledRejectionHandler);

			return;
        }

		if (action.equals(Action.togglePlayPause))
			action = playlistManager.isPlaying() ? Action.pause : Action.play;

		if (action.equals(Action.play)) {
        	playlistManager
				.resume()
				.next(this::restartObservable)
				.next(lazyPlaybackStartedBroadcaster.getObject())
				.next(this::handlePlaybackStarted)
				.error(UnhandledRejectionHandler);

        	return;
        }

		if (action.equals(Action.pause)) {
			pausePlayback(true);
			return;
		}

		if (action.equals(Action.seekTo)) {
			final int playlistPosition = intent.getIntExtra(Action.Bag.playlistPosition, -1);
			if (playlistPosition < 0) return;

			final int filePosition = intent.getIntExtra(Action.Bag.startPos, -1);
			if (filePosition < 0) return;

			playlistManager
				.changePosition(playlistPosition, filePosition)
				.next(this::broadcastChangedFile)
				.error(UnhandledRejectionHandler);

			return;
		}

		if (action.equals(Action.previous)) {
			playlistManager.skipToPrevious().next(this::broadcastChangedFile).error(UnhandledRejectionHandler);
			return;
		}

		if (action.equals(Action.next)) {
			playlistManager.skipToNext().next(this::broadcastChangedFile).error(UnhandledRejectionHandler);
			return;
		}

		if (action.equals(Action.stopWaitingForConnection)) {
        	PollConnection.Instance.get(this).stopPolling();
			return;
		}

		if (action.equals(Action.addFileToPlaylist)) {
			final int fileKey = intent.getIntExtra(Action.Bag.playlistPosition, -1);
			if (fileKey < 0) return;

			playlistManager
				.addFile(new ServiceFile(fileKey))
				.next(Dispatch.toContext(library -> {
					Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show();
					return library;
				}, this))
				.error(UnhandledRejectionHandler);

			return;
		}

		if (action.equals(Action.removeFileAtPositionFromPlaylist)) {
			final int filePosition = intent.getIntExtra(Action.Bag.filePosition, -1);
			if (filePosition < -1) return;

			playlistManager.removeFileAtPosition(filePosition).error(UnhandledRejectionHandler);
		}
	}

	private Observable<PositionedPlaybackFile> handlePlaybackStarted(Observable<PositionedPlaybackFile> positionedPlaybackFileObservable) {
		isPlaying = true;
		NowPlayingActivity.startNowPlayingActivity(this);

		return positionedPlaybackFileObservable;
	}

	private Observable<PositionedPlaybackFile> restartObservable(Observable<PositionedPlaybackFile> positionedPlaybackFileObservable) {
		if (positionedPlaybackFile != null) {
			positionedPlaybackFileObservable =
				Observable
					.just(positionedPlaybackFile)
					.concatWith(positionedPlaybackFileObservable);
		}

		return observePlaybackFileChanges(positionedPlaybackFileObservable);
	}

	private Observable<PositionedPlaybackFile> observePlaybackFileChanges(Observable<PositionedPlaybackFile> observable) {
		if (playbackFileChangedSubscription != null)
			playbackFileChangedSubscription.dispose();

		if (playbackFileChangesConnection != null)
			playbackFileChangesConnection.dispose();

		final ConnectableObservable<PositionedPlaybackFile> playbackFileChangesPublisher = observable.publish();

		final ConnectableObservable<PositionedPlaybackFile> playbackFileChangesReplayer = playbackFileChangesPublisher.replay(1);

		playbackFileChangedSubscription =
			playbackFileChangesPublisher.subscribe(
				this::changePositionedPlaybackFile,
				this::uncaughtExceptionHandler,
				this::onPlaylistPlaybackComplete);

		playbackFileChangesConnection = playbackFileChangesPublisher.connect();

		playbackFileChangesReplayer.connect();

		return playbackFileChangesReplayer;
	}

	private void pausePlayback(boolean isUserInterrupted) {
		isPlaying = false;

		updateClientBitmap(null);

		stopNotification();

		if (isUserInterrupted && areListenersRegistered) unregisterListeners();

		if (playlistManager == null) return;

		playlistManager.pause();

		if (positionedPlaybackFile != null)
			lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistPause, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlaybackFile.asPositionedFile());

		if (filePositionSubscription != null)
			filePositionSubscription.dispose();
	}

	private void uncaughtExceptionHandler(Throwable exception) {
		if (exception instanceof MediaPlayerException) {
			handleMediaPlayerException((MediaPlayerException)exception);
			return;
		}

		logger.error("An uncaught error has occurred!", exception);
	}

	private void handleMediaPlayerException(MediaPlayerException exception) {
		final long currentErrorTime = System.currentTimeMillis();
		// Stop handling errors if more than the max errors has occurred
		if (++numberOfErrors > maxErrors) {
			// and the last error time is less than the error count reset duration
			if (currentErrorTime <= lastErrorTime + errorCountResetDuration)
				return;

			// reset the error count if enough time has elapsed to reset the error count
			numberOfErrors = 1;
		}

		lastErrorTime = currentErrorTime;

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setOngoing(true);
		// Add intent for canceling waiting for connection to come back
		final Intent intent = new Intent(this, PlaybackService.class);
		intent.setAction(Action.stopWaitingForConnection);
		PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pi);

		builder.setContentTitle(getText(R.string.lbl_waiting_for_connection));
		builder.setContentText(getText(R.string.lbl_click_to_cancel));
		notifyForeground(builder);
		
		final PollConnection checkConnection = PollConnection.Instance.get(this);

		checkConnection.addOnConnectionRegainedListener(connectionRegainedListener.getObject());
		checkConnection.addOnPollingCancelledListener(onPollingCancelledListener.getObject());
		
		checkConnection.startPolling();
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// resume playback
			if (lazyPlaylistVolumeManager.isInitialized())
				lazyPlaylistVolumeManager.getObject().setVolume(1.0f);

			if (!playlistManager.isPlaying())
				playlistManager
					.resume()
					.next(this::restartObservable)
					.next(lazyPlaybackStartedBroadcaster.getObject());

			return;
		}
		
		if (playlistManager == null || !playlistManager.isPlaying()) return;

	    switch (focusChange) {
	        case AudioManager.AUDIOFOCUS_LOSS:
		        // Lost focus for an unbounded amount of time: stop playback and release media player
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
		        // Lost focus but it will be regained... cannot release resources
		        pausePlayback(false);
	            return;
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				// Lost focus for a short time, but it's ok to keep playing
				// at an attenuated level
				if (lazyPlaylistVolumeManager.isInitialized())
					lazyPlaylistVolumeManager.getObject().setVolume(0.2f);
	    }
	}

	private void changePositionedPlaybackFile(PositionedPlaybackFile positionedPlaybackFile) {
		this.positionedPlaybackFile = positionedPlaybackFile;

		broadcastChangedFile(positionedPlaybackFile.asPositionedFile());
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onFileStart, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlaybackFile.asPositionedFile());

		final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

		if (filePositionSubscription != null)
			filePositionSubscription.dispose();

		final Disposable localFilePositionSubscription = filePositionSubscription =
			Observable
				.interval(1, TimeUnit.SECONDS)
				.map(i -> playbackHandler.getCurrentPosition())
				.distinctUntilChanged()
				.subscribe(new TrackPositionBroadcaster(this, positionedPlaybackFile));

		playbackHandler
			.promisePlayback()
			.next(runCarelessly(handler -> {
				lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onFileComplete, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlaybackFile.asPositionedFile());
				localFilePositionSubscription.dispose();
			}));
		
		if (!areListenersRegistered) registerListeners();
		registerRemoteClientControl();
		
		// Set the notification area
		final Intent viewIntent = new Intent(this, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		final PendingIntent pi = PendingIntent.getActivity(this, 0, viewIntent, 0);

		new ImageProvider(this, SessionConnection.getSessionConnectionProvider(), cachedFilePropertiesProvider)
			.promiseFileBitmap(positionedPlaybackFile.getServiceFile())
			.next(Dispatch.toContext(this::updateClientBitmap, this))
			.error(e -> {
				logger.warn("There was an error getting the image for the file with id `" + positionedPlaybackFile.getServiceFile().getKey() + "`", e);
				return null;
			});

		cachedFilePropertiesProvider
			.promiseFileProperties(positionedPlaybackFile.getServiceFile().getKey())
			.next(Dispatch.toContext(fileProperties -> {
				final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
				final String name = fileProperties.get(FilePropertiesProvider.NAME);

				final Builder builder = new Builder(this);
				builder.setOngoing(true);
				builder.setContentTitle(String.format(getString(R.string.title_svc_now_playing), getText(R.string.app_name)));
				builder.setContentText(artist + " - " + name);
				builder.setContentIntent(pi);
				notifyForeground(builder);

				final String album = fileProperties.get(FilePropertiesProvider.ALBUM);
				final long duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);
				final String trackNumberString = fileProperties.get(FilePropertiesProvider.TRACK);
				final Integer trackNumber = trackNumberString != null && !trackNumberString.isEmpty() ? Integer.valueOf(trackNumberString) : null;

				final MetadataEditor metaData = remoteControlClient.getObject().editMetadata(true);
				metaData.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, artist);
				metaData.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, album);
				metaData.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, name);
				metaData.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration);
				if (trackNumber != null)
					metaData.putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, trackNumber.longValue());
				metaData.apply();

				return null;
			}, this))
		.error(Dispatch.toContext(exception -> {
			final Builder builder = new Builder(this);
			builder.setOngoing(true);
			builder.setContentTitle(String.format(getString(R.string.title_svc_now_playing), getText(R.string.app_name)));
			builder.setContentText(getText(R.string.lbl_error_getting_file_properties));
			builder.setContentIntent(pi);
			notifyForeground(builder);

			return null;
		}, this));
	}

	private Void updateClientBitmap(Bitmap bitmap) {
		if (remoteClientBitmap == bitmap) return null;

		final MetadataEditor metaData = remoteControlClient.getObject().editMetadata(false);
		metaData.putBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, bitmap).apply();

		// Track the remote client bitmap and recycle it in case the remote control client
		// does not properly recycle the bitmap
		if (remoteClientBitmap != null) remoteClientBitmap.recycle();
		remoteClientBitmap = bitmap;

		return null;
	}

	private Void broadcastChangedFile(PositionedFile positionedFile) {
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistChange, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedFile);
		return null;
	}

	private void onPlaylistPlaybackComplete() {
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistStop, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlaybackFile.asPositionedFile());

		updateClientBitmap(null);

		stopNotification();
		if (areListenersRegistered) unregisterListeners();
	}
		
	@Override
	public void onDestroy() {
		stopNotification();

		localBroadcastManagerLazy.getObject().unregisterReceiver(onLibraryChanged);

		if (playlistPlaybackBootstrapper != null) {
			try {
				playlistPlaybackBootstrapper.close();
			} catch (IOException e) {
				logger.warn("There was an error closing the prepared playback bootstrapper", e);
			}
		}

		if (playlistManager != null) {
			try {
				playlistManager.close();
			} catch (Exception e) {
				logger.warn("There was an error closing the prepared playback queue", e);
			}
		}
		
		if (areListenersRegistered) unregisterListeners();
		
		if (remoteControlReceiver.isInitialized())
			audioManagerLazy.getObject().unregisterMediaButtonEventReceiver(remoteControlReceiver.getObject());
		if (remoteControlClient.isInitialized())
			audioManagerLazy.getObject().unregisterRemoteControlClient(remoteControlClient.getObject());

		if (remoteClientBitmap != null) {
			remoteClientBitmap.recycle();
			remoteClientBitmap = null;
		}

		if (playbackFileChangedSubscription != null)
			playbackFileChangedSubscription.dispose();

		if (filePositionSubscription != null)
			filePositionSubscription.dispose();
	}

	/* End Event Handlers */
	
	/* Begin Binder Code */
	
	@Override
	public IBinder onBind(Intent intent) {
		return lazyBinder.getObject();
	}

	private final Lazy<IBinder> lazyBinder = new Lazy<>(() -> new GenericBinder<>(this));
	/* End Binder Code */


	private static class Action {
		private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(Action.class);

		/* String constant actions */
		private static final String launchMusicService = magicPropertyBuilder.buildProperty("launchMusicService");
		private static final String play = magicPropertyBuilder.buildProperty("play");
		private static final String pause = magicPropertyBuilder.buildProperty("pause");
		private static final String togglePlayPause = magicPropertyBuilder.buildProperty("togglePlayPause");
		private static final String repeating = magicPropertyBuilder.buildProperty("repeating");
		private static final String completing = magicPropertyBuilder.buildProperty("completing");
		private static final String previous = magicPropertyBuilder.buildProperty("previous");
		private static final String next = magicPropertyBuilder.buildProperty("next");
		private static final String seekTo = magicPropertyBuilder.buildProperty("seekTo");
		private static final String stopWaitingForConnection = magicPropertyBuilder.buildProperty("stopWaitingForConnection");
		private static final String addFileToPlaylist = magicPropertyBuilder.buildProperty("addFileToPlaylist");
		private static final String removeFileAtPositionFromPlaylist = magicPropertyBuilder.buildProperty("removeFileAtPositionFromPlaylist");

		private static final Set<String> validActions = new HashSet<>(Arrays.asList(new String[]{
				launchMusicService,
				play,
				pause,
				togglePlayPause,
				previous,
				next,
				seekTo,
				repeating,
				completing,
				stopWaitingForConnection,
				addFileToPlaylist,
				removeFileAtPositionFromPlaylist
		}));

		private static final Set<String> playbackStartingActions = new HashSet<>(Arrays.asList(new String[]{
			launchMusicService,
			play,
			togglePlayPause
		}));

		private static class Bag {
			private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(Bag.class);

			/* Bag constants */
			private static final String playlistPosition = magicPropertyBuilder.buildProperty("playlistPosition");
			private static final String filePlaylist = magicPropertyBuilder.buildProperty("filePlaylist");
			private static final String startPos = magicPropertyBuilder.buildProperty("startPos");
			private static final String filePosition = magicPropertyBuilder.buildProperty("filePosition");
		}
	}
}
