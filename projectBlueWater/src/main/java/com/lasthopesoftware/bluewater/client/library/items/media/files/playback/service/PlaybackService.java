/**
 * 
 */
package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service;


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
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection.BuildingSessionConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaybackController;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingPauseListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnPlaylistStateControlErrorListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.receivers.RemoteControlReceiver;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.listener.ListenerThrower;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.lazyj.AbstractLazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
		private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(Action.class);

		/* String constant actions */
		private static final String launchMusicService = magicPropertyBuilder.buildProperty("launchMusicService");
		private static final String play = magicPropertyBuilder.buildProperty("play");
		private static final String pause = magicPropertyBuilder.buildProperty("pause");
		private static final String previous = magicPropertyBuilder.buildProperty("previous");
		private static final String next = magicPropertyBuilder.buildProperty("next");
		private static final String seekTo = magicPropertyBuilder.buildProperty("seekTo");
		private static final String stopWaitingForConnection = magicPropertyBuilder.buildProperty("stopWaitingForConnection");
		private static final String initializePlaylist = magicPropertyBuilder.buildProperty("initializePlaylist");
		private static final String addFileToPlaylist = magicPropertyBuilder.buildProperty("addFileToPlaylist");
		private static final String removeFileAtPositionFromPlaylist = magicPropertyBuilder.buildProperty("removeFileAtPositionFromPlaylist");

		private static final Set<String> validActions = new HashSet<>(Arrays.asList(new String[]{
				launchMusicService,
				play,
				pause,
				previous,
				next,
				seekTo,
				stopWaitingForConnection,
				initializePlaylist,
				addFileToPlaylist,
				removeFileAtPositionFromPlaylist
		}));

		private static class Bag {
			private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(Bag.class);

			/* Bag constants */
			private static final String fileKey = magicPropertyBuilder.buildProperty("fileKey");
			private static final String filePlaylist = magicPropertyBuilder.buildProperty("filePlaylist");
			private static final String startPos = magicPropertyBuilder.buildProperty("startPos");
			private static final String filePosition = magicPropertyBuilder.buildProperty("filePosition");
		}
	}

	public static class PlaylistEvents {
		private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(PlaylistEvents.class);

		public static final String onPlaylistChange = magicPropertyBuilder.buildProperty("onPlaylistChange");
		public static final String onPlaylistStart = magicPropertyBuilder.buildProperty("onPlaylistStart");
		public static final String onPlaylistStop = magicPropertyBuilder.buildProperty("onPlaylistStop");
		public static final String onPlaylistPause = magicPropertyBuilder.buildProperty("onPlaylistPause");

		public static class PlaybackFileParameters {
			private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(PlaybackFileParameters.class);

			public static final String fileKey = magicPropertyBuilder.buildProperty("fileKey");
			public static final String filePosition = magicPropertyBuilder.buildProperty("filePosition");
			public static final String fileDuration = magicPropertyBuilder.buildProperty("fileDuration");
			public static final String isPlaying = magicPropertyBuilder.buildProperty("isPlaying");
		}

		public static class PlaylistParameters {
			public static final String playlistPosition = MagicPropertyBuilder.buildMagicPropertyName(PlaylistParameters.class, "playlistPosition");
		}
	}


	private static Intent getNewSelfIntent(final Context context, String action) {
		final Intent newIntent = new Intent(context, PlaybackService.class);
		newIntent.setAction(action);
		return newIntent;
	}

	/* Begin streamer intent helpers */
	public static void initializePlaylist(final Context context, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.initializePlaylist);
		svcIntent.putExtra(Action.Bag.filePlaylist, serializedFileList);
		context.startService(svcIntent);
	}

	public static void initializePlaylist(final Context context, int filePos, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.initializePlaylist);
		svcIntent.putExtra(Action.Bag.fileKey, filePos);
		svcIntent.putExtra(Action.Bag.filePlaylist, serializedFileList);
		context.startService(svcIntent);
	}

	public static void initializePlaylist(final Context context, int filePos, int fileProgress, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.initializePlaylist);
		svcIntent.putExtra(Action.Bag.fileKey, filePos);
		svcIntent.putExtra(Action.Bag.filePlaylist, serializedFileList);
		svcIntent.putExtra(Action.Bag.startPos, fileProgress);
		context.startService(svcIntent);
	}

	public static void launchMusicService(final Context context, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.launchMusicService);
		svcIntent.putExtra(Action.Bag.filePlaylist, serializedFileList);
		context.startService(svcIntent);
	}

	public static void launchMusicService(final Context context, int filePos, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.launchMusicService);
		svcIntent.putExtra(Action.Bag.fileKey, filePos);
		svcIntent.putExtra(Action.Bag.filePlaylist, serializedFileList);
		context.startService(svcIntent);
	}

	public static void launchMusicService(final Context context, int filePos, int fileProgress, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.launchMusicService);
		svcIntent.putExtra(Action.Bag.fileKey, filePos);
		svcIntent.putExtra(Action.Bag.filePlaylist, serializedFileList);
		svcIntent.putExtra(Action.Bag.startPos, fileProgress);
		context.startService(svcIntent);
	}

	public static void seekTo(final Context context, int filePos) {
		final Intent svcIntent = getNewSelfIntent(context, Action.seekTo);
		svcIntent.putExtra(Action.Bag.fileKey, filePos);
		context.startService(svcIntent);
	}

	public static void seekTo(final Context context, int filePos, int fileProgress) {
		final Intent svcIntent = getNewSelfIntent(context, Action.seekTo);
		svcIntent.putExtra(Action.Bag.fileKey, filePos);
		svcIntent.putExtra(Action.Bag.startPos, fileProgress);
		context.startService(svcIntent);
	}

	public static void play(final Context context) {
		context.startService(getNewSelfIntent(context, Action.play));
	}

	public static void pause(final Context context) {
		context.startService(getNewSelfIntent(context, Action.pause));
	}

	public static void next(final Context context) {
		context.startService(getNewSelfIntent(context, Action.next));
	}

	public static void previous(final Context context) {
		context.startService(getNewSelfIntent(context, Action.previous));
	}

	public static void setIsRepeating(final Context context, final boolean isRepeating) {
		LibrarySession.GetActiveLibrary(context, result -> {
			if (result == null) return;
			result.setRepeating(isRepeating);
			LibrarySession.SaveLibrary(context, result, result1 -> {
				if (playlistController != null) playlistController.setIsRepeating(isRepeating);
			});
		});
	}

	public static void addFileToPlaylist(final Context context, int fileKey) {
		final Intent intent = getNewSelfIntent(context, Action.addFileToPlaylist);
		intent.putExtra(Action.Bag.fileKey, fileKey);
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
	private static final String SCROBBLE_DROID_INTENT = "net.jjc1138.android.scrobbler.action.MUSIC_STATUS";

	private static final int notificationId = 42;
	private static int startId;

	private static final int maxErrors = 3;
	private static final int errorCountResetDuration = 1000;

	private WifiLock wifiLock = null;
	private NotificationManager notificationManager;
	private AudioManager audioManager;
	private ComponentName remoteControlReceiver;
	private RemoteControlClient remoteControlClient;
	private Bitmap remoteClientBitmap = null;
	private int numberOfErrors = 0;
	private long lastErrorTime = 0;
	
	// State dependent static variables
	private static volatile String playlistString;
	// Declare as volatile so that every thread has the same version of the playlist controllers
	private static volatile PlaybackController playlistController;
	
	private static boolean areListenersRegistered = false;
	private static boolean isNotificationForeground = false;

	private static final Object syncPlaylistControllerObject = new Object();
	
	private static final HashSet<OnNowPlayingChangeListener> onStreamingChangeListeners = new HashSet<>();
	
	/* Begin Events */
	public static void addOnStreamingChangeListener(OnNowPlayingChangeListener listener) {
		synchronized(onStreamingChangeListeners) {
			onStreamingChangeListeners.add(listener);
		}
	}

	public static void removeOnStreamingChangeListener(OnNowPlayingChangeListener listener) {
		synchronized(onStreamingChangeListeners) {
			onStreamingChangeListeners.remove(listener);
		}
	}

	private void throwChangeEvent(final PlaybackController controller, final IPlaybackFile filePlayer) {
		synchronized(onStreamingChangeListeners) {
            ListenerThrower.throwListeners(onStreamingChangeListeners, parameter -> parameter.onNowPlayingChange(controller, filePlayer));
		}

		sendPlaybackBroadcast(PlaylistEvents.onPlaylistChange, controller, filePlayer);
	}
	
	private void sendPlaybackBroadcast(final String broadcastMessage, final PlaybackController playbackController, final IPlaybackFile playbackFile) {
		final Intent playbackBroadcastIntent = new Intent(broadcastMessage);

		playbackBroadcastIntent
				.putExtra(PlaylistEvents.PlaylistParameters.playlistPosition, playbackController.getCurrentPosition())
				.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, playbackFile.getFile().getKey())
				.putExtra(PlaylistEvents.PlaybackFileParameters.filePosition, playbackFile.getCurrentPosition())
				.putExtra(PlaylistEvents.PlaybackFileParameters.isPlaying, playbackFile.isPlaying());

		final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), playbackFile.getFile().getKey());
		filePropertiesProvider.onComplete(fileProperties -> {
			playbackBroadcastIntent
					.putExtra(PlaylistEvents.PlaybackFileParameters.fileDuration, FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties));

			localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
		}).onError(error -> {
			playbackBroadcastIntent
					.putExtra(PlaylistEvents.PlaybackFileParameters.fileDuration, -1);

			localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
			return true;
		}).execute();
	}

	/* End Events */

	public static IPlaybackFile getCurrentPlaybackFile() {
		synchronized(syncPlaylistControllerObject) {
			return playlistController != null ? playlistController.getCurrentPlaybackFile() : null;
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

	private LocalBroadcastManager localBroadcastManager;

	private final AbstractLazy<Runnable> connectionRegainedListener = new AbstractLazy<Runnable>() {
		@Override
		protected final Runnable initialize() throws Exception {
			return () -> {
				if (playlistController != null && !playlistController.isPlaying()) {
					stopSelf(startId);
					return;
				}

				LibrarySession.GetActiveLibrary(PlaybackService.this, result -> startPlaylist(result.getSavedTracksString(), result.getNowPlayingId(), result.getNowPlayingProgress()));

			};
		}
	};

	private final AbstractLazy<Runnable> onPollingCancelledListener = new AbstractLazy<Runnable>() {
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
			stopSelf(startId);
		}
	};
		
	private void restorePlaylistControllerFromStorage(final OneParameterRunnable<Boolean> onPlaylistRestored) {

		LibrarySession.GetActiveLibrary(this, library -> {
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

				SessionConnection.refresh(PlaybackService.this);
			});
	}

	private void startPlaylist(final String playlistString, final int filePos, final int fileProgress) {
		startPlaylist(playlistString, filePos, fileProgress, null);
	}
	
	private void startPlaylist(final String playlistString, final int filePos, final int fileProgress, final Runnable onPlaylistStarted) {
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
		LibrarySession.GetActiveLibrary(this, result -> {
			synchronized (syncPlaylistControllerObject) {
				logger.info("Initializing playlist.");
				PlaybackService.playlistString = playlistString;

				// First try to get the playlist string from the database
				if (PlaybackService.playlistString == null || PlaybackService.playlistString.isEmpty())
					PlaybackService.playlistString = result.getSavedTracksString();

				result.setSavedTracksString(PlaybackService.playlistString);
				LibrarySession.SaveLibrary(PlaybackService.this, result, savedLibrary -> {
					if (playlistController != null) {
						playlistController.pause();
						playlistController.release();
					}

					playlistController = new PlaybackController(PlaybackService.this, SessionConnection.getSessionConnectionProvider(), PlaybackService.playlistString);

					playlistController.setIsRepeating(savedLibrary.isRepeating());
					playlistController.addOnNowPlayingChangeListener(PlaybackService.this);
					playlistController.addOnNowPlayingStopListener(PlaybackService.this);
					playlistController.addOnNowPlayingPauseListener(PlaybackService.this);
					playlistController.addOnPlaylistStateControlErrorListener(PlaybackService.this);
					playlistController.addOnNowPlayingStartListener(PlaybackService.this);

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
			final PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
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
		final PollConnection pollConnection = PollConnection.Instance.get(this);
		if (connectionRegainedListener.isInitialized())
			pollConnection.removeOnConnectionRegainedListener(connectionRegainedListener.getObject());
		if (onPollingCancelledListener.isInitialized())
			pollConnection.removeOnPollingCancelledListener(onPollingCancelledListener.getObject());
		
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
		
		if (!SessionConnection.isBuilt()) {
			// TODO this should probably be its own service soon
			final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

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

			handleBuildStatusChange(SessionConnection.build(this), intent);
			
			return START_NOT_STICKY;
		}
		
		actOnIntent(intent);
		
		return START_NOT_STICKY;
	}
	
	private void handleBuildStatusChange(final int status, final Intent intentToRun) {
		final Builder notifyBuilder = new Builder(this);
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
		if (action == null) return;

		if (action.equals(Action.launchMusicService)) {
			startPlaylist(intent.getStringExtra(Action.Bag.filePlaylist), intent.getIntExtra(Action.Bag.fileKey, -1), intent.getIntExtra(Action.Bag.startPos, 0), () -> NowPlayingActivity.startNowPlayingActivity(PlaybackService.this));
			
			return;
        }
		
		if (action.equals(Action.initializePlaylist)) {
        	initializePlaylist(intent.getStringExtra(Action.Bag.filePlaylist), intent.getIntExtra(Action.Bag.fileKey, -1), intent.getIntExtra(Action.Bag.startPos, 0), null);
        	return;
        }
		
		if (action.equals(Action.play)) {
        	if (playlistController == null) {
        		restorePlaylistForIntent(intent);
        		return;
        	}
        	
        	if (playlistController.resume()) return;
        	
        	LibrarySession.GetActiveLibrary(this, result -> startPlaylist(result.getSavedTracksString(), result.getNowPlayingId(), result.getNowPlayingProgress()));
        	
        	return;
        }
		
		if (action.equals(Action.seekTo)) {
        	if (playlistController == null) {
        		restorePlaylistForIntent(intent);
        		return;
        	}
        	
        	playlistController.seekTo(intent.getIntExtra(Action.Bag.fileKey, 0), intent.getIntExtra(Action.Bag.startPos, 0));
        	return;
        }
		
		if (action.equals(Action.previous)) {
        	if (playlistController == null) {
        		restorePlaylistForIntent(intent);
        		return;
        	}
        	
        	playlistController.seekTo(playlistController.getCurrentPosition() > 0 ? playlistController.getCurrentPosition() - 1 : playlistController.getPlaylist().size() - 1);
        	return;
        }
		
		if (action.equals(Action.next)) {
        	if (playlistController == null) {
        		restorePlaylistForIntent(intent);
        		return;
        	}
        	
        	playlistController.seekTo(playlistController.getCurrentPosition() < playlistController.getPlaylist().size() - 1 ? playlistController.getCurrentPosition() + 1 : 0);
        	return;
        }
		
		if (playlistController != null && action.equals(Action.pause)) {
        	pausePlayback(true);
        	return;
        }
		
		if (action.equals(Action.stopWaitingForConnection)) {
        	PollConnection.Instance.get(this).stopPolling();
			return;
		}

		if (action.equals(Action.addFileToPlaylist)) {
			final int fileKey = intent.getIntExtra(Action.Bag.fileKey, -1);
			if (fileKey < 0) return;

			synchronized (syncPlaylistControllerObject) {
				if (playlistController != null)
					playlistController.addFile(new File(fileKey));
			}

			LibrarySession.GetActiveLibrary(this, result -> {
				if (result == null) return;
				String newFileString = result.getSavedTracksString();
				if (!newFileString.endsWith(";")) newFileString += ";";
				newFileString += fileKey + ";";
				result.setSavedTracksString(newFileString);

				LibrarySession.SaveLibrary(PlaybackService.this, result, result1 -> Toast.makeText(PlaybackService.this, PlaybackService.this.getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show());
			});

			return;
		}

		if (action.equals(Action.removeFileAtPositionFromPlaylist)) {
			final int filePosition = intent.getIntExtra(Action.Bag.filePosition, -1);
			if (filePosition < -1) return;

			LibrarySession.GetActiveLibrary(this, library -> {
				if (library == null) return;

				// It could take quite a while to split string and put it back together, so let's do it
				// in a background task
				(new AsyncTask<Void, Void, String>() {
					@Override
					protected String doInBackground(Void... params) {
						synchronized (syncPlaylistControllerObject) {
							if (playlistController != null) {
								playlistController.removeFile(filePosition);
								return playlistController.getPlaylistString();
							}
						}

						final List<IFile> savedTracks = FileStringListUtilities.parseFileStringList(library.getSavedTracksString());
						savedTracks.remove(filePosition);
						return FileStringListUtilities.serializeFileStringList(savedTracks);
					}

					@Override
					protected void onPostExecute(String s) {
						super.onPostExecute(s);

						library.setSavedTracksString(s);

						LibrarySession.SaveLibrary(PlaybackService.this, library);
					}
				}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			});
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

		localBroadcastManager.registerReceiver(onLibraryChanged, new IntentFilter(LibrarySession.libraryChosenEvent));

		registerRemoteClientControl();
	}

	@Override
	public void onPlaylistStateControlError(PlaybackController controller, IPlaybackFile filePlayer) {
		saveStateToLibrary(controller, filePlayer);

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
			if (playlistController != null) {
				playlistController.setVolume(1.0f);
	    		if (playlistController.isPlaying() || playlistController.resume()) return;
			}

			restorePlaylistControllerFromStorage(result -> {
				if (result) playlistController.resume();
			});

			return;
		}
		
		if (playlistController == null || !playlistController.isPlaying()) return;
		
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
	            playlistController.setVolume(0.2f);
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
		LibrarySession.GetActiveLibrary(this, result -> {

			result.setSavedTracksString(controller.getPlaylistString());
			result.setNowPlayingId(controller.getCurrentPosition());
			result.setNowPlayingProgress(filePlayer.getCurrentPosition());

			LibrarySession.SaveLibrary(PlaybackService.this, result);
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

		final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), playingFile.getKey());
		filePropertiesProvider.onComplete(fileProperties -> {
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

			final Intent pebbleIntent = new Intent(PEBBLE_NOTIFY_INTENT);
			pebbleIntent.putExtra("artist", artist);
			pebbleIntent.putExtra("album", album);
			pebbleIntent.putExtra("track", name);

			sendBroadcast(pebbleIntent);

			final Intent scrobbleDroidIntent = getScrobbleIntent(true);
			scrobbleDroidIntent.putExtra("artist", artist);
			scrobbleDroidIntent.putExtra("album", album);
			scrobbleDroidIntent.putExtra("track", name);
			scrobbleDroidIntent.putExtra("secs", (int) (duration / 1000));
			if (trackNumber != null)
				scrobbleDroidIntent.putExtra("tracknumber", trackNumber.intValue());

			sendBroadcast(scrobbleDroidIntent);

			if (remoteControlClient == null) return;

			final MetadataEditor metaData = remoteControlClient.editMetadata(true);
			metaData.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, artist);
			metaData.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, album);
			metaData.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, name);
			metaData.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration);
			if (trackNumber != null)
				metaData.putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, trackNumber.longValue());
			metaData.apply();

			if (Build.VERSION.SDK_INT < 19) return;

			ImageProvider
					.getImage(PlaybackService.this, SessionConnection.getSessionConnectionProvider(), playingFile.getKey())
					.onComplete((owner1, bitmap) -> {
						// Track the remote client bitmap and recycle it in case the remote control client
						// does not properly recycle the bitmap
						if (remoteClientBitmap != null) remoteClientBitmap.recycle();
						remoteClientBitmap = bitmap;

						final MetadataEditor metaData1 = remoteControlClient.editMetadata(false);
						metaData1.putBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, bitmap).apply();
					})
					.execute();
		}).onError(exception -> {
			final Builder builder = new Builder(this);
			builder.setOngoing(true);
			builder.setContentTitle(String.format(getString(R.string.title_svc_now_playing), getText(R.string.app_name)));
			builder.setContentText(getText(R.string.lbl_error_getting_file_properties));
			builder.setContentIntent(pi);
			notifyForeground(builder);

			return true;
		}).execute();

		sendPlaybackBroadcast(PlaylistEvents.onPlaylistStart, controller, filePlayer);
	}
		
	@Override
	public void onDestroy() {
		stopNotification();

		localBroadcastManager.unregisterReceiver(onLibraryChanged);

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
