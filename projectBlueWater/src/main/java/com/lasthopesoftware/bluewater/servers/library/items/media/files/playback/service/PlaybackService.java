/**
 * 
 */
package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service;


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
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseArray;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection.BuildingSessionConnectionStatus;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingActivity;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingPauseListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnPlaylistStateControlErrorListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.receivers.RemoteControlReceiver;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;
import com.lasthopesoftware.bluewater.shared.listener.ListenerThrower;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.OneParameterRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * @author david
 *
 */
public class PlaybackService extends Service implements
	OnAudioFocusChangeListener, 
	OnNowPlayingChangeListener, 
	OnNowPlayingStartListener,
	OnNowPlayingStopListener,
	OnNowPlayingPauseListener, 
	OnPlaylistStateControlErrorListener
{
	private static final Logger logger = LoggerFactory.getLogger(PlaybackService.class);

	private static class Action {
		/* String constant actions */

		private static final String LAUNCH_MUSIC_SERVICE = SpecialValueHelpers.buildMagicPropertyName(Action.class, "LAUNCH_MUSIC_SERVICE");
		private static final String PLAY = SpecialValueHelpers.buildMagicPropertyName(Action.class, "PLAY");
		private static final String PAUSE = SpecialValueHelpers.buildMagicPropertyName(Action.class, "PAUSE");
		private static final String PREVIOUS = SpecialValueHelpers.buildMagicPropertyName(Action.class, "PREVIOUS");
		private static final String NEXT = SpecialValueHelpers.buildMagicPropertyName(Action.class, "NEXT");
		private static final String SEEK_TO = SpecialValueHelpers.buildMagicPropertyName(Action.class, "SEEK_TO");
		private static final String SYSTEM_PAUSE = SpecialValueHelpers.buildMagicPropertyName(Action.class, "SYSTEM_PAUSE");
		private static final String STOP_WAITING_FOR_CONNECTION = SpecialValueHelpers.buildMagicPropertyName(Action.class, "STOP_WAITING_FOR_CONNECTION");
		private static final String INITIALIZE_PLAYLIST = SpecialValueHelpers.buildMagicPropertyName(Action.class, "INITIALIZE_PLAYLIST");

		private static final Set<String> validActions = new HashSet<>(Arrays.asList(new String[]{
				LAUNCH_MUSIC_SERVICE,
				PLAY,
				PAUSE,
				PREVIOUS,
				NEXT,
				SEEK_TO,
				STOP_WAITING_FOR_CONNECTION,
				INITIALIZE_PLAYLIST
		}));

		private static class Bag {
			/* Bag constants */
			private static final String FILE_KEY = SpecialValueHelpers.buildMagicPropertyName(Bag.class, "FILE_KEY");
			private static final String FILE_PLAYLIST = SpecialValueHelpers.buildMagicPropertyName(Bag.class, "FILE_PLAYLIST");
			private static final String START_POS = SpecialValueHelpers.buildMagicPropertyName(Bag.class, "START_POS");
		}
	}

	public static class PlaylistEvents {
		public static final String onPlaylistChange = SpecialValueHelpers.buildMagicPropertyName(PlaylistEvents.class, "onPlaylistChange");
		public static final String onPlaylistStart = SpecialValueHelpers.buildMagicPropertyName(PlaylistEvents.class, "onPlaylistStart");
		public static final String onPlaylistStop = SpecialValueHelpers.buildMagicPropertyName(PlaylistEvents.class, "onPlaylistStop");
		public static final String onPlaylistPause = SpecialValueHelpers.buildMagicPropertyName(PlaylistEvents.class, "onPlaylistPause");

		public static class PlaybackFileParameters {
			public static final String fileKey = SpecialValueHelpers.buildMagicPropertyName(PlaybackFileParameters.class, "fileKey");
			public static final String filePosition = SpecialValueHelpers.buildMagicPropertyName(PlaybackFileParameters.class, "filePosition");
			public static final String fileDuration = SpecialValueHelpers.buildMagicPropertyName(PlaybackFileParameters.class, "fileDuration");
			public static final String isPlaying = SpecialValueHelpers.buildMagicPropertyName(PlaybackFileParameters.class, "isPlaying");
		}

		public static class PlaylistParameters {
			public static final String playlistPosition = SpecialValueHelpers.buildMagicPropertyName(PlaylistParameters.class, "playlistPosition");
		}
	}


	private static Intent getNewSelfIntent(final Context context, String action) {
		final Intent newIntent = new Intent(context, PlaybackService.class);
		newIntent.setAction(action);
		return newIntent;
	}

	/* Begin streamer intent helpers */
	public static void initializePlaylist(final Context context, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.INITIALIZE_PLAYLIST);
		svcIntent.putExtra(Action.Bag.FILE_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
	}

	public static void initializePlaylist(final Context context, int filePos, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.INITIALIZE_PLAYLIST);
		svcIntent.putExtra(Action.Bag.FILE_KEY, filePos);
		svcIntent.putExtra(Action.Bag.FILE_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
	}

	public static void initializePlaylist(final Context context, int filePos, int fileProgress, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.INITIALIZE_PLAYLIST);
		svcIntent.putExtra(Action.Bag.FILE_KEY, filePos);
		svcIntent.putExtra(Action.Bag.FILE_PLAYLIST, serializedFileList);
		svcIntent.putExtra(Action.Bag.START_POS, fileProgress);
		context.startService(svcIntent);
	}

	public static void launchMusicService(final Context context, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.LAUNCH_MUSIC_SERVICE);
		svcIntent.putExtra(Action.Bag.FILE_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
	}

	public static void launchMusicService(final Context context, int filePos, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.LAUNCH_MUSIC_SERVICE);
		svcIntent.putExtra(Action.Bag.FILE_KEY, filePos);
		svcIntent.putExtra(Action.Bag.FILE_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
	}

	public static void launchMusicService(final Context context, int filePos, int fileProgress, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.LAUNCH_MUSIC_SERVICE);
		svcIntent.putExtra(Action.Bag.FILE_KEY, filePos);
		svcIntent.putExtra(Action.Bag.FILE_PLAYLIST, serializedFileList);
		svcIntent.putExtra(Action.Bag.START_POS, fileProgress);
		context.startService(svcIntent);
	}

	public static void seekTo(final Context context, int filePos) {
		final Intent svcIntent = getNewSelfIntent(context, Action.SEEK_TO);
		svcIntent.putExtra(Action.Bag.FILE_KEY, filePos);
		context.startService(svcIntent);
	}

	public static void seekTo(final Context context, int filePos, int fileProgress) {
		final Intent svcIntent = getNewSelfIntent(context, Action.SEEK_TO);
		svcIntent.putExtra(Action.Bag.FILE_KEY, filePos);
		svcIntent.putExtra(Action.Bag.START_POS, fileProgress);
		context.startService(svcIntent);
	}

	public static void play(final Context context) {
		context.startService(getNewSelfIntent(context, Action.PLAY));
	}

	public static void pause(final Context context) {
		context.startService(getNewSelfIntent(context, Action.PAUSE));
	}

	public static void next(final Context context) {
		context.startService(getNewSelfIntent(context, Action.NEXT));
	}

	public static void previous(final Context context) {
		context.startService(getNewSelfIntent(context, Action.PREVIOUS));
	}

	public static void setIsRepeating(final Context context, final boolean isRepeating) {
		LibrarySession.GetActiveLibrary(context, (owner, result) -> {
			if (result == null) return;
			result.setRepeating(isRepeating);
			LibrarySession.SaveLibrary(context, result, (owner1, result1) -> {
				if (playlistController != null) playlistController.setIsRepeating(isRepeating);
			});
		});
	}

	/* End streamer intent helpers */
	
	/* Miscellaneous programming related string constants */
	private static final String PEBBLE_NOTIFY_INTENT = "com.getpebble.action.NOW_PLAYING";
	private static final String WIFI_LOCK_SVC_NAME =  "project_blue_water_svc_lock";
	private static final String SCROBBLE_DROID_INTENT = "net.jjc1138.android.scrobbler.action.MUSIC_STATUS";

	private static final int notificationId = 42;
	private static int startId;
	private WifiLock wifiLock = null;
	private NotificationManager notificationManager;
	private PlaybackService playbackService;
	private AudioManager audioManager;
	private ComponentName remoteControlReceiver;
	private RemoteControlClient remoteControlClient;
	private Bitmap remoteClientBitmap = null;
	private volatile boolean hasAudioFocus = true;
	
	// State dependent static variables
	private static volatile String playlistString;
	// Declare as volatile so that every thread has the same version of the playlist controllers
	private static volatile PlaybackController playlistController;
	
	private static boolean areListenersRegistered = false;
	private static boolean isNotificationForeground = false;
	
	private static final Object syncHandlersObject = new Object();
	private static final Object syncPlaylistControllerObject = new Object();
	
	private static final HashSet<OnNowPlayingChangeListener> onStreamingChangeListeners = new HashSet<>();

	private Runnable connectionRegainedListener;
	
	private Runnable onPollingCancelledListener;

	private LocalBroadcastManager localBroadcastManager;
	
	/* Begin Events */
	public static void addOnStreamingChangeListener(OnNowPlayingChangeListener listener) {
		onStreamingChangeListeners.add(listener);
	}

	public static void removeOnStreamingChangeListener(OnNowPlayingChangeListener listener) {
		synchronized(syncHandlersObject) {
			if (onStreamingChangeListeners.contains(listener))
				onStreamingChangeListeners.remove(listener);
		}
	}

	private void throwChangeEvent(final PlaybackController controller, final IPlaybackFile filePlayer) {
		synchronized(syncHandlersObject) {
            ListenerThrower.throwListeners(onStreamingChangeListeners, parameter -> parameter.onNowPlayingChange(controller, filePlayer));
		}

		sendPlaybackBroadcast(PlaylistEvents.onPlaylistChange, controller, filePlayer);
	}
	
	private void sendPlaybackBroadcast(final String broadcastMessage, final PlaybackController playbackController, final IPlaybackFile playbackFile) {
		final Intent playbackBroadcastIntent = new Intent(broadcastMessage);

		int duration = -1;
		try {
			duration = playbackFile.getDuration();
		} catch (IOException io) {
			logger.warn("There was an error getting the playback file duration", io);
		}

		playbackBroadcastIntent
				.putExtra(PlaylistEvents.PlaylistParameters.playlistPosition, playbackController.getCurrentPosition())
				.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, playbackFile.getFile().getKey())
				.putExtra(PlaylistEvents.PlaybackFileParameters.filePosition, playbackFile.getCurrentPosition())
				.putExtra(PlaylistEvents.PlaybackFileParameters.fileDuration, duration)
				.putExtra(PlaylistEvents.PlaybackFileParameters.isPlaying, playbackFile.isPlaying());

		localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
	}

	/* End Events */
		
	public static PlaybackController getPlaylistController() {
		synchronized(syncPlaylistControllerObject) {
			return playlistController;
		}
	}

	public static boolean isPlaying() {
		synchronized (syncPlaylistControllerObject) {
			return playlistController != null && playlistController.isPlaying();
		}
	}

	public static int getCurrentPlayingFileKey() {
		synchronized (syncPlaylistControllerObject) {
			return playlistController != null && playlistController.getCurrentPlaybackFile() != null ? playlistController.getCurrentPlaybackFile().getFile().getKey() : -1;
		}
	}

	public static int getCurrentPlaylistPosition() {
		synchronized (syncPlaylistControllerObject) {
			return playlistController != null ? playlistController.getCurrentPosition() : -1;
		}
	}
	
	public PlaybackService() {
		super();
		playbackService = this;
	}
		
	private void restorePlaylistControllerFromStorage(final OneParameterRunnable<Boolean> onPlaylistRestored) {

		LibrarySession.GetActiveLibrary(playbackService, (owner, library) -> {
				if (library == null) return;

				final Runnable onPlaylistInitialized = () -> onPlaylistRestored.run(true);

				final BroadcastReceiver buildSessionReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						final int result = intent.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1);
						if (!SessionConnection.completeConditions.contains(result)) return;

						localBroadcastManager.unregisterReceiver(this);

						if (result != BuildingSessionConnectionStatus.BuildingSessionComplete) {
							onPlaylistRestored.run(false);
							return;
						}

						initializePlaylist(library, onPlaylistInitialized);
					}
				};

				localBroadcastManager.registerReceiver(buildSessionReceiver, new IntentFilter(SessionConnection.buildSessionBroadcast));

				final BroadcastReceiver refreshBroadcastReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						localBroadcastManager.unregisterReceiver(this);

						if (!intent.getBooleanExtra(SessionConnection.isRefreshSuccessfulStatus, false))
							return;

						localBroadcastManager.unregisterReceiver(buildSessionReceiver);
						initializePlaylist(library, onPlaylistInitialized);
					}
				};

				localBroadcastManager.registerReceiver(refreshBroadcastReceiver, new IntentFilter(SessionConnection.refreshSessionBroadcast));

				SessionConnection.refresh(playbackService);
			});
	}

	private void startPlaylist(final String playlistString, final int filePos, final int fileProgress) {
		startPlaylist(playlistString, filePos, fileProgress, null);
	}
	
	private void startPlaylist(final String playlistString, final int filePos, final int fileProgress, final Runnable onPlaylistStarted) {
		hasAudioFocus = false;

		notifyStartingService();

		// If the playlist has changed, change that
		if (playlistController == null || !playlistString.equals(PlaybackService.playlistString)) {
			initializePlaylist(playlistString, () -> {
				startPlaylist(playlistString, filePos, fileProgress, onPlaylistStarted);

			});
			return;
		}
        
		logger.info("Starting playback");
        playlistController.startAt(filePos, fileProgress);
        
        if (onPlaylistStarted != null) onPlaylistStarted.run();
	}

	private void initializePlaylist(final Library library, final Runnable onPlaylistControllerInitialized) {
		initializePlaylist(library.getSavedTracksString(), library.getNowPlayingId(), library.getNowPlayingProgress(), onPlaylistControllerInitialized);
	}

	private void initializePlaylist(final String playlistString, final int filePos, final int fileProgress, final Runnable onPlaylistControllerInitialized) {
		initializePlaylist(playlistString, () -> {
			if (!playlistString.isEmpty())
				playlistController.seekTo(filePos, fileProgress);

			if (onPlaylistControllerInitialized != null)
				onPlaylistControllerInitialized.run();
		});
	}
	
	private void initializePlaylist(final String playlistString, final Runnable onPlaylistControllerInitialized) {		
		LibrarySession.GetActiveLibrary(playbackService, (owner, result) -> {
			synchronized (syncPlaylistControllerObject) {
				logger.info("Initializing playlist.");
				PlaybackService.playlistString = playlistString;

				// First try to get the playlist string from the database
				if (PlaybackService.playlistString == null || PlaybackService.playlistString.isEmpty())
					PlaybackService.playlistString = result.getSavedTracksString();

				result.setSavedTracksString(PlaybackService.playlistString);
				LibrarySession.SaveLibrary(playbackService, result, (owner1, result1) -> {
					if (playlistController != null) {
						playlistController.pause();
						playlistController.release();
					}

					playlistController = new PlaybackController(playbackService, SessionConnection.getSessionConnectionProvider(), PlaybackService.playlistString);

					playlistController.setIsRepeating(result1.isRepeating());
					playlistController.addOnNowPlayingChangeListener(playbackService);
					playlistController.addOnNowPlayingStopListener(playbackService);
					playlistController.addOnNowPlayingPauseListener(playbackService);
					playlistController.addOnPlaylistStateControlErrorListener(playbackService);
					playlistController.addOnNowPlayingStartListener(playbackService);

					onPlaylistControllerInitialized.run();
				});
			}
		});
	}
	
	private void pausePlayback(boolean isUserInterrupted) {
		stopNotification();
		
		if (playlistController == null || !playlistController.isPlaying()) return;

		if (isUserInterrupted && areListenersRegistered) unregisterListeners();
		playlistController.pause();
	}
	
	private void notifyForeground(Builder notificationBuilder) {
		notificationBuilder.setSmallIcon(R.drawable.clearstream_logo_dark);
		final Notification notification = notificationBuilder.build();

		if (!isNotificationForeground) {
			startForeground(notificationId, notification);
			isNotificationForeground = true;
			return;
		}
		
		notificationManager.notify(notificationId, notification);
	}
	
	private void stopNotification() {
		stopForeground(true);
		isNotificationForeground = false;
		notificationManager.cancel(notificationId);
	}

	private void notifyStartingService() {
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setOngoing(true);
		builder.setContentTitle(String.format(getString(R.string.lbl_starting_service), getString(R.string.app_name)));

		notifyForeground(builder);
	}
	
	private void registerListeners() {
		audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
				
		wifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_SVC_NAME);
        wifiLock.acquire();
		
        registerRemoteClientControl();
        
		areListenersRegistered = true;
	}
	
	private void registerRemoteClientControl() {
		if (audioManager == null) return;
		
		if (remoteControlReceiver == null)
			remoteControlReceiver = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
		
		audioManager.registerMediaButtonEventReceiver(remoteControlReceiver);
        
		if (remoteControlClient == null) {
	        // build the PendingIntent for the remote control client
			final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			mediaButtonIntent.setComponent(remoteControlReceiver);
			final PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(playbackService, 0, mediaButtonIntent, 0);
			// create and register the remote control client
			remoteControlClient = new RemoteControlClient(mediaPendingIntent);
			remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			remoteControlClient.setTransportControlFlags(
					RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
							RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
							RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
							RemoteControlClient.FLAG_KEY_MEDIA_STOP);
		}
		
		audioManager.registerRemoteControlClient(remoteControlClient);
	}
	
	private void unregisterListeners() {
		audioManager.abandonAudioFocus(this);
		
		// release the wifilock if we still have it
		if (wifiLock != null) {
			if (wifiLock.isHeld()) wifiLock.release();
			wifiLock = null;
		}
		final PollConnection pollConnection = PollConnection.Instance.get(playbackService);
		if (connectionRegainedListener != null)
			pollConnection.removeOnConnectionRegainedListener(connectionRegainedListener);
		if (onPollingCancelledListener != null)
			pollConnection.removeOnPollingCancelledListener(onPollingCancelledListener);
		
		areListenersRegistered = false;
	}
	
	/* Begin Event Handlers */
	
	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	
	public int onStartCommand(final Intent intent, int flags, int startId) {
		// Should be modified to save its state locally in the future.
		PlaybackService.startId = startId;

		if (!Action.validActions.contains(intent.getAction())) {
			stopSelf(startId);
			return START_NOT_STICKY;
		}
		
		playbackService = this;
		
		if (!SessionConnection.isBuilt()) {
			// TODO this should probably be its own service soon
			final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(playbackService);

			final BroadcastReceiver buildSessionReceiver  = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					final int buildStatus = intent.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1);
					handleBuildStatusChange(buildStatus, intent);

					if (SessionConnection.completeConditions.contains(buildStatus))
						localBroadcastManager.unregisterReceiver(this);
				}
			};

			localBroadcastManager.registerReceiver(buildSessionReceiver, new IntentFilter(SessionConnection.buildSessionBroadcast));

			handleBuildStatusChange(SessionConnection.build(playbackService), intent);
			
			return START_NOT_STICKY;
		}
		
		actOnIntent(intent);
		
		return START_NOT_STICKY;
	}
	
	private void handleBuildStatusChange(final int status, final Intent intentToRun) {
		final Builder notifyBuilder = new Builder(playbackService);
		notifyBuilder.setContentTitle(getText(R.string.title_svc_connecting_to_server));
		switch (status) {
		case BuildingSessionConnectionStatus.GettingLibrary:
			notifyBuilder.setContentText(getText(R.string.lbl_getting_library_details));
			break;
		case BuildingSessionConnectionStatus.GettingLibraryFailed:
//			notifyBuilder.setContentText(getText(R.string.lbl_please_connect_to_valid_server));
			stopSelf(startId);
//			launchActivityDelayed(selectServerIntent);
			return;
		case BuildingSessionConnectionStatus.BuildingConnection:
			notifyBuilder.setContentText(getText(R.string.lbl_connecting_to_server_library));
			break;
		case BuildingSessionConnectionStatus.BuildingConnectionFailed:
//			lblConnectionStatus.setText(R.string.lbl_error_connecting_try_again);
//			launchActivityDelayed(selectServerIntent);
			stopSelf(startId);
			return;
		case BuildingSessionConnectionStatus.GettingView:
			notifyBuilder.setContentText(getText(R.string.lbl_getting_library_views));
			return;
		case BuildingSessionConnectionStatus.GettingViewFailed:
//			lblConnectionStatus.setText(R.string.lbl_library_no_views);
//			launchActivityDelayed(selectServerIntent);
			stopSelf(startId);
			return;
		case BuildingSessionConnectionStatus.BuildingSessionComplete:
			stopNotification();
			actOnIntent(intentToRun);
			return;
		}
		notifyForeground(notifyBuilder);
	}
	
	private void actOnIntent(final Intent intent) {
		if (intent == null) {
			pausePlayback(true);
			return;
		}
		
		final String action = intent.getAction(); 
		if (action.equals(Action.LAUNCH_MUSIC_SERVICE)) {
			startPlaylist(intent.getStringExtra(Action.Bag.FILE_PLAYLIST), intent.getIntExtra(Action.Bag.FILE_KEY, -1), intent.getIntExtra(Action.Bag.START_POS, 0), new Runnable() {
				
				@Override
				public void run() {
					ViewUtils.CreateNowPlayingView(playbackService);
				}
			});
			
			return;
        }
		
		if (action.equals(Action.INITIALIZE_PLAYLIST)) {
        	initializePlaylist(intent.getStringExtra(Action.Bag.FILE_PLAYLIST), intent.getIntExtra(Action.Bag.FILE_KEY, -1), intent.getIntExtra(Action.Bag.START_POS, 0), null);
        	return;
        }
		
		if (action.equals(Action.PLAY)) {
        	if (playlistController == null) {
        		restorePlaylistForIntent(intent);
        		return;
        	}
        	
        	if (playlistController.resume()) return;
        	
        	LibrarySession.GetActiveLibrary(playbackService, (owner, result) -> startPlaylist(result.getSavedTracksString(), result.getNowPlayingId(), result.getNowPlayingProgress()));
        	
        	return;
        }
		
		if (action.equals(Action.SEEK_TO)) {
        	if (playlistController == null) {
        		restorePlaylistForIntent(intent);
        		return;
        	}
        	
        	playlistController.seekTo(intent.getIntExtra(Action.Bag.FILE_KEY, 0), intent.getIntExtra(Action.Bag.START_POS, 0));
        	return;
        }
		
		if (action.equals(Action.PREVIOUS)) {
        	if (playlistController == null) {
        		restorePlaylistForIntent(intent);
        		return;
        	}
        	
        	playlistController.seekTo(playlistController.getCurrentPosition() > 0 ? playlistController.getCurrentPosition() - 1 : playlistController.getPlaylist().size() - 1);
        	return;
        }
		
		if (action.equals(Action.NEXT)) {
        	if (playlistController == null) {
        		restorePlaylistForIntent(intent);
        		return;
        	}
        	
        	playlistController.seekTo(playlistController.getCurrentPosition() < playlistController.getPlaylist().size() - 1 ? playlistController.getCurrentPosition() + 1 : 0);
        	return;
        }
		
		if (playlistController != null && action.equals(Action.PAUSE)) {
        	pausePlayback(true);
        	return;
        }
		
		if (action.equals(Action.STOP_WAITING_FOR_CONNECTION)) {
        	PollConnection.Instance.get(playbackService).stopPolling();
		}
	}
	
	private void restorePlaylistForIntent(final Intent intent) {
		notifyStartingService();

		restorePlaylistControllerFromStorage(result -> {
			if (result) {
				actOnIntent(intent);

				if (playlistController != null && playlistController.isPlaying()) return;
			}

			stopNotification();
		});
	}
	
	@Override
    public void onCreate() {
		notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		
		registerRemoteClientControl();
	}

	@Override
	public void onPlaylistStateControlError(PlaybackController controller, IPlaybackFile filePlayer) {
		saveStateToLibrary(controller, filePlayer);
		
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setOngoing(true);
		// Add intent for canceling waiting for connection to come back
		final Intent intent = new Intent(playbackService, PlaybackService.class);
		intent.setAction(Action.STOP_WAITING_FOR_CONNECTION);
		PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pi);

		builder.setContentTitle(getText(R.string.lbl_waiting_for_connection));
		builder.setContentText(getText(R.string.lbl_click_to_cancel));
		notifyForeground(builder);
		
		final PollConnection checkConnection = PollConnection.Instance.get(playbackService);
		
		if (connectionRegainedListener == null) {
			connectionRegainedListener = () -> {
				if (playlistController != null && !playlistController.isPlaying()) {
					stopSelf(startId);
					return;
				}

				LibrarySession.GetActiveLibrary(playbackService, (owner, result) -> startPlaylist(result.getSavedTracksString(), result.getNowPlayingId(), result.getNowPlayingProgress()));

			};
		}
		
		checkConnection.addOnConnectionRegainedListener(connectionRegainedListener);
		
		if (onPollingCancelledListener == null) {
			onPollingCancelledListener = () -> {
				unregisterListeners();
				stopSelf(startId);
			};
		}
		checkConnection.addOnPollingCancelledListener(onPollingCancelledListener);
		
		checkConnection.startPolling();
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			hasAudioFocus = true;

			// resume playback
			if (playlistController != null) {
				playlistController.setVolume(1.0f);
	    		if (playlistController.isPlaying()) return;

				if (playlistController.resume()) return;
			}

			restorePlaylistControllerFromStorage(result -> {
				if (result && hasAudioFocus) playlistController.resume();
			});

			return;
		}
		
		if (playlistController == null) return;
		
	    switch (focusChange) {
        	// Lost focus for an unbounded amount of time: stop playback and release media player
	        case AudioManager.AUDIOFOCUS_LOSS:
	        	hasAudioFocus = false;
	        // Lost focus but it will be regained... cannot release resources
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	        	if (playlistController.isPlaying()) pausePlayback(false);
	            return;
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	            // Lost focus for a short time, but it's ok to keep playing
	            // at an attenuated level
	            if (playlistController.isPlaying()) playlistController.setVolume(0.2f);
	    }
	}
	
	@Override
	public void onNowPlayingStop(PlaybackController controller, IPlaybackFile filePlayer) {
		saveStateToLibrary(controller, filePlayer);
		
		sendPlaybackBroadcast(PlaylistEvents.onPlaylistStop, controller, filePlayer);
		
		stopNotification();
		if (areListenersRegistered) unregisterListeners();
		
		controller.seekTo(0);
		
		sendBroadcast(getScrobbleIntent(false));
	}
	
	@Override
	public void onNowPlayingPause(PlaybackController controller, IPlaybackFile filePlayer) {
		saveStateToLibrary(controller, filePlayer);
		
		stopNotification();
		
		sendPlaybackBroadcast(PlaylistEvents.onPlaylistPause, controller, filePlayer);
		
		sendBroadcast(getScrobbleIntent(false));
	}

	private static Intent getScrobbleIntent(final boolean isPlaying) {
		final Intent scrobbleDroidIntent = new Intent(SCROBBLE_DROID_INTENT);
		scrobbleDroidIntent.putExtra("playing", isPlaying);
		
		return scrobbleDroidIntent;
	}

	@Override
	public void onNowPlayingChange(PlaybackController controller, IPlaybackFile filePlayer) {
		saveStateToLibrary(controller, filePlayer);		
		throwChangeEvent(controller, filePlayer);
	}
	
	private void saveStateToLibrary(final PlaybackController controller, final IPlaybackFile filePlayer) {
		LibrarySession.GetActiveLibrary(playbackService, (owner, result) -> {

			result.setSavedTracksString(controller.getPlaylistString());
			result.setNowPlayingId(controller.getCurrentPosition());
			result.setNowPlayingProgress(filePlayer.getCurrentPosition());

			LibrarySession.SaveLibrary(playbackService, result);
		});
	}
	
	@Override
	public void onNowPlayingStart(PlaybackController controller, IPlaybackFile filePlayer) {
		final IFile playingFile = filePlayer.getFile();
		
		if (!areListenersRegistered) registerListeners();
		registerRemoteClientControl();
		
		// Set the notification area
		final Intent viewIntent = new Intent(this, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		final PendingIntent pi = PendingIntent.getActivity(this, 0, viewIntent, 0);
		
		final FluentTask<Void, Void, String> getNotificationPropertiesTask = new FluentTask<Void, Void, String>() {

			@Override
			protected String executeInBackground(Void... params) {
				try {
					return playingFile.getProperty(FilePropertiesProvider.ARTIST) + " - " + playingFile.getValue();
				} catch (IOException e) {
					setException(e);
					return null;
				}
			}
		};

		getNotificationPropertiesTask.onComplete(result -> {
			final Builder builder = new Builder(playbackService);
			builder.setOngoing(true);
			builder.setContentTitle(String.format(getString(R.string.title_svc_now_playing), getText(R.string.app_name)).toLowerCase());
			builder.setContentText(result == null ? getText(R.string.lbl_error_getting_file_properties) : result);
			builder.setContentIntent(pi);
			notifyForeground(builder);
		});
		
		getNotificationPropertiesTask.execute();
		
		final FluentTask<Void, Void, SparseArray<Object>> getTrackPropertiesTask = new FluentTask<Void, Void, SparseArray<Object>>() {

			@Override
			protected SparseArray<Object> executeInBackground(Void... params) {
				final SparseArray<Object> result = new SparseArray<>(5);

				try {
					result.put(MediaMetadataRetriever.METADATA_KEY_ARTIST, playingFile.getProperty(FilePropertiesProvider.ARTIST));
					result.put(MediaMetadataRetriever.METADATA_KEY_ALBUM, playingFile.getProperty(FilePropertiesProvider.ALBUM));
					result.put(MediaMetadataRetriever.METADATA_KEY_TITLE, playingFile.getValue());
					result.put(MediaMetadataRetriever.METADATA_KEY_DURATION, (long) playingFile.getDuration());
					final String trackNumber = playingFile.getProperty(FilePropertiesProvider.TRACK);
					if (trackNumber != null && !trackNumber.isEmpty())
						result.put(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, Integer.valueOf(trackNumber));
					return result;
				} catch (IOException e) {
					setException(e);
					return new SparseArray<>();
				}
			}
		};

		getTrackPropertiesTask.onComplete((owner, result) -> {

			final String artist = (String) result.get(MediaMetadataRetriever.METADATA_KEY_ARTIST);
			final String album = (String) result.get(MediaMetadataRetriever.METADATA_KEY_ALBUM);
			final String title = (String) result.get(MediaMetadataRetriever.METADATA_KEY_TITLE);
			final Long duration = (Long) result.get(MediaMetadataRetriever.METADATA_KEY_DURATION);
			final Integer trackNumber = (Integer) result.get(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);

			final Intent pebbleIntent = new Intent(PEBBLE_NOTIFY_INTENT);
			pebbleIntent.putExtra("artist", artist);
			pebbleIntent.putExtra("album", album);
			pebbleIntent.putExtra("track", title);

			sendBroadcast(pebbleIntent);

			final Intent scrobbleDroidIntent = getScrobbleIntent(true);
			scrobbleDroidIntent.putExtra("artist", artist);
			scrobbleDroidIntent.putExtra("album", album);
			scrobbleDroidIntent.putExtra("track", title);
			scrobbleDroidIntent.putExtra("secs", (int) (duration / 1000));
			if (trackNumber != null)
				scrobbleDroidIntent.putExtra("tracknumber", trackNumber.intValue());

			sendBroadcast(scrobbleDroidIntent);

			if (remoteControlClient == null) return;

			final MetadataEditor metaData = remoteControlClient.editMetadata(true);
			metaData.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, artist);
			metaData.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, album);
			metaData.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, title);
			metaData.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration.longValue());
			if (trackNumber != null)
				metaData.putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, trackNumber.longValue());
			metaData.apply();

			if (Build.VERSION.SDK_INT < 19) return;

			ImageProvider
					.getImage(playbackService, SessionConnection.getSessionConnectionProvider(), playingFile)
					.onComplete((owner1, bitmap) -> {
						// Track the remote client bitmap and recycle it in case the remote control client
						// does not properly recycle the bitmap
						if (remoteClientBitmap != null) remoteClientBitmap.recycle();
						remoteClientBitmap = bitmap;

						final MetadataEditor metaData1 = remoteControlClient.editMetadata(false);
						metaData1.putBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, bitmap).apply();
					})
					.execute();

		});

		getTrackPropertiesTask.onError(e -> true);

		getTrackPropertiesTask.execute();
		
		sendPlaybackBroadcast(PlaylistEvents.onPlaylistStart, controller, filePlayer);
	}
		
	@Override
	public void onDestroy() {
		stopNotification();
		
		if (playlistController != null) {
			if (playlistController.getCurrentPlaybackFile() != null)
				saveStateToLibrary(playlistController, playlistController.getCurrentPlaybackFile());

			playlistController.release();
			playlistController = null;
		}
		
		if (areListenersRegistered) unregisterListeners();
		
		if (audioManager != null) {
			if (remoteControlReceiver != null)
				audioManager.unregisterMediaButtonEventReceiver(remoteControlReceiver);
			if (remoteControlClient != null)
				audioManager.unregisterRemoteControlClient(remoteControlClient);
		}
		
		if (remoteClientBitmap != null) {
			remoteClientBitmap.recycle();
			remoteClientBitmap = null;
		}
		
		playlistString = null;
	}

	/* End Event Handlers */
	
	/* Begin Binder Code */
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private final IBinder mBinder = new GenericBinder<>(this);
	/* End Binder Code */

}
