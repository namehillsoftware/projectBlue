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
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.broadcasters.LocalPlaybackBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.receivers.RemoteControlReceiver;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.lazyj.AbstractSynchronousLazy;
import com.vedsoft.lazyj.AbstractThreadLocalLazy;
import com.vedsoft.lazyj.ILazy;
import com.vedsoft.lazyj.Lazy;

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
public class PlaybackService extends Service implements OnAudioFocusChangeListener
{
	private static final Logger logger = LoggerFactory.getLogger(PlaybackService.class);

	private static Intent getNewSelfIntent(final Context context, String action) {
		final Intent newIntent = new Intent(context, PlaybackService.class);
		newIntent.setAction(action);
		return newIntent;
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
		context.startService(getNewSelfIntent(context, Action.repeating));
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

	private static final Lazy<IPositionedFileQueueProvider> lazyPositionedFileQueueProvider = new Lazy<>(PositionedFileQueueProvider::new);

	private WifiLock wifiLock = null;
	private final Lazy<NotificationManager> notificationManagerLazy = new Lazy<>(() -> (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE));
	private final Lazy<AudioManager> audioManagerLazy = new Lazy<>(() -> (AudioManager)getSystemService(Context.AUDIO_SERVICE));
	private final Lazy<LocalBroadcastManager> localBroadcastManagerLazy = new Lazy<>(() -> LocalBroadcastManager.getInstance(this));
	private ComponentName remoteControlReceiver;
	private RemoteControlClient remoteControlClient;
	private Bitmap remoteClientBitmap = null;
	private int numberOfErrors = 0;
	private long lastErrorTime = 0;
	
	// State dependent static variables
	private volatile String playlistString;
	
	private static boolean areListenersRegistered = false;
	private static boolean isNotificationForeground = false;

	private PlaybackPlaylistStateManager playbackPlaylistStateManager;

	private PreparedPlaybackQueue preparedPlaybackQueue;

	private volatile PositionedPlaybackFile positionedPlaybackFile;

	private final ILazy<IPlaybackBroadcaster> lazyPlaybackBroadcaster = new AbstractThreadLocalLazy<IPlaybackBroadcaster>() {
		@Override
		protected IPlaybackBroadcaster initialize() throws Exception {
			return new LocalPlaybackBroadcaster(PlaybackService.this);
		}
	};

	private final AbstractSynchronousLazy<Runnable> connectionRegainedListener = new AbstractSynchronousLazy<Runnable>() {
		@Override
		protected final Runnable initialize() throws Exception {
			return () -> {
				if (playbackPlaylistStateManager == null) {
					stopSelf(startId);
					return;
				}

				LibrarySession
					.getActiveLibrary(PlaybackService.this)
					.thenPromise(result ->
						FileStringListUtilities
							.promiseParsedFileStringList(result.getSavedTracksString())
							.then(VoidFunc.running(playlist ->
								playbackPlaylistStateManager.startPlaylist(playlist, result.getNowPlayingId(), result.getNowPlayingProgress()))));
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
				playbackPlaylistStateManager.close();
			} catch (IOException e) {
				logger.error("There was an error closing the playbackPlaylistStateManager", e);
			}

			playbackPlaylistStateManager = new PlaybackPlaylistStateManager(PlaybackService.this, chosenLibrary, new PositionedFileQueueProvider());
		}
	};
	
	private void pausePlayback(boolean isUserInterrupted) {
		stopNotification();

		if (playbackPlaylistStateManager == null) return;

		if (isUserInterrupted && areListenersRegistered) unregisterListeners();

		playbackPlaylistStateManager.pause();

		stopNotification();

		if (positionedPlaybackFile != null)
			lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistPause, positionedPlaybackFile);

		sendBroadcast(getScrobbleIntent(false));
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
				
		wifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_SVC_NAME);
        wifiLock.acquire();
		
        registerRemoteClientControl();
        
		areListenersRegistered = true;
	}
	
	private void registerRemoteClientControl() {
		if (remoteControlReceiver == null) {
			remoteControlReceiver = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
			audioManagerLazy.getObject().registerMediaButtonEventReceiver(remoteControlReceiver);
		}
        
		if (remoteControlClient == null) {
	        // build the PendingIntent for the remote control client
			final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			mediaButtonIntent.setComponent(remoteControlReceiver);
			final PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
			// create and register the remote control client
			remoteControlClient = new RemoteControlClient(mediaPendingIntent);
			remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			remoteControlClient.setTransportControlFlags(
				RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
				RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
				RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
				RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
				RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
				RemoteControlClient.FLAG_KEY_MEDIA_STOP);
		}
		
		audioManagerLazy.getObject().registerRemoteControlClient(remoteControlClient);
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
	public final int onStartCommand(final Intent intent, int flags, int startId) {
		// Should be modified to save its state locally in the future.
		PlaybackService.startId = startId;

		if (!Action.validActions.contains(intent.getAction())) {
			stopSelf(startId);
			return START_NOT_STICKY;
		}
		
		if (SessionConnection.isBuilt()) {
			actOnIntent(intent);
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

		if (action.equals(Action.repeating)) {
			playbackPlaylistStateManager.playRepeatedly();
			return;
		}

		if (action.equals(Action.completing)) {
			playbackPlaylistStateManager.playToCompletion();
			return;
		}

		if (action.equals(Action.launchMusicService)) {
			FileStringListUtilities
				.promiseParsedFileStringList(intent.getStringExtra(Action.Bag.filePlaylist))
				.then(VoidFunc.running(playlist -> playbackPlaylistStateManager.startPlaylist(playlist, intent.getIntExtra(Action.Bag.fileKey, -1), intent.getIntExtra(Action.Bag.startPos, 0))));

			return;
        }

		if (action.equals(Action.play)) {
        	if (playbackPlaylistStateManager == null) {
				restorePlaylistForIntent(intent);
        		return;
        	}

			playbackPlaylistStateManager.resume();

        	return;
        }
		
		if (action.equals(Action.seekTo)) {
        	playbackPlaylistStateManager.changePosition(intent.getIntExtra(Action.Bag.fileKey, 0), intent.getIntExtra(Action.Bag.startPos, 0));
        	return;
        }
		
		if (action.equals(Action.previous)) {
        	if (positionedPlaybackFile != null) {
				final int position =  positionedPlaybackFile.getPosition();
				playbackPlaylistStateManager.changePosition(position > 0 ? position - 1 : 0, 0);
				return;
			}

        	LibrarySession
				.getActiveLibrary(this)
				.then(VoidFunc.running(library -> {
					final int position =  library.getNowPlayingId();
					playbackPlaylistStateManager.changePosition(position > 0 ? position - 1 : 0, 0);
				}));

			return;
        }
		
		if (action.equals(Action.next)) {
        	if (playlist != null && positionedPlaybackFile != null) {
				final int newPosition =  positionedPlaybackFile.getPosition();
				final int playlistSize = playlist.size();
				playbackPlaylistStateManager.changePosition(newPosition < playlistSize - 1 ? newPosition + 1 : 0, 0);
        		return;
        	}

			LibrarySession
				.getActiveLibrary(this)
				.then(VoidFunc.running(library -> {
					final int newPosition =  library.getNowPlayingId();
					FileStringListUtilities
						.promiseParsedFileStringList(library.getSavedTracksString())
						.then(VoidFunc.running(savedTracks -> {
							final int playlistSize = savedTracks.size();
							playbackPlaylistStateManager.changePosition(newPosition < playlistSize - 1 ? newPosition + 1 : 0, 0);
						}));
				}));

        	return;
        }
		
		if (playbackPlaylistStateManager != null && action.equals(Action.pause)) {
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

			playbackPlaylistStateManager
				.addFile(new File(fileKey))
				.then(library -> {
					Toast.makeText(PlaybackService.this, PlaybackService.this.getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show();
					return library;
				});

			return;
		}

		if (action.equals(Action.removeFileAtPositionFromPlaylist)) {
			final int filePosition = intent.getIntExtra(Action.Bag.filePosition, -1);
			if (filePosition < -1) return;

			playbackPlaylistStateManager.removeFileAtPosition(filePosition);
		}
	}
	
	private void restorePlaylistForIntent(final Intent intent) {
		notifyStartingService();

		playbackPlaylistStateManager.restorePlaylistFromStorage()
			.then(VoidFunc.running(result -> {
				if (result) {
					actOnIntent(intent);

					if (playbackPlaylistStateManager != null && playbackPlaylistStateManager.isPlaying()) return;
				}

				stopNotification();
			}));
	}
	
	@Override
    public final void onCreate() {
		LibrarySession
			.getActiveLibrary(this)
			.then(library -> {
				playbackPlaylistStateManager = new PlaybackPlaylistStateManager(this, library.getId(), new PositionedFileQueueProvider());

				localBroadcastManagerLazy.getObject().registerReceiver(onLibraryChanged, new IntentFilter(LibrarySession.libraryChosenEvent));

				return library;
			});

		registerRemoteClientControl();
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
			if (playbackPlaylistStateManager != null) {
				playlistPlayer.setVolume(1.0f);
	    		if (playbackPlaylistStateManager.isPlaying()) {
					playbackPlaylistStateManager.resume();
					return;
				}
			}

			playbackPlaylistStateManager
				.restorePlaylistFromStorage()
				.then(VoidFunc.running(result -> {
					if (result) playbackPlaylistStateManager.resume();
				}));

			return;
		}
		
		if (playbackPlaylistStateManager == null || !playbackPlaylistStateManager.isPlaying()) return;

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
	            playlistPlayer.setVolume(0.2f);
	    }
	}

	private static Intent getScrobbleIntent(final boolean isPlaying) {
		final Intent scrobbleDroidIntent = new Intent(SCROBBLE_DROID_INTENT);
		scrobbleDroidIntent.putExtra("playing", isPlaying);
		
		return scrobbleDroidIntent;
	}

	private void changePositionedPlaybackFile(PositionedPlaybackFile positionedPlaybackFile) {
		this.positionedPlaybackFile = positionedPlaybackFile;

		positionedPlaybackFile
			.getPlaybackHandler()
			.promisePlayback()
			.then(VoidFunc.running(handler -> lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onFileComplete, positionedPlaybackFile)));

		final IFile playingFile = playlist.get(positionedPlaybackFile.getPosition());
		
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
					.onComplete((bitmap) -> {
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

		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistChange, positionedPlaybackFile);
	}
		
	@Override
	public void onDestroy() {
		stopNotification();

		localBroadcastManagerLazy.getObject().unregisterReceiver(onLibraryChanged);

		if (playbackPlaylistStateManager!= null) {
			try {
				playbackPlaylistStateManager.close();
			} catch (IOException e) {
				logger.warn("There was an error closing the prepared playback queue", e);
			}
		}
		
		if (areListenersRegistered) unregisterListeners();
		
		if (remoteControlReceiver != null)
			audioManagerLazy.getObject().unregisterMediaButtonEventReceiver(remoteControlReceiver);
		if (remoteControlClient != null)
			audioManagerLazy.getObject().unregisterRemoteControlClient(remoteControlClient);
		
		if (remoteClientBitmap != null) {
			remoteClientBitmap.recycle();
			remoteClientBitmap = null;
		}
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
		public static final String onFileComplete = magicPropertyBuilder.buildProperty("onFileComplete");

		public static class PlaybackFileParameters {
			private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(PlaybackFileParameters.class);

			public static final String fileKey = magicPropertyBuilder.buildProperty("fileKey");
			public static final String fileLibraryId = magicPropertyBuilder.buildProperty("fileLibraryId");
			public static final String filePosition = magicPropertyBuilder.buildProperty("filePosition");
			public static final String fileDuration = magicPropertyBuilder.buildProperty("fileDuration");
			public static final String isPlaying = magicPropertyBuilder.buildProperty("isPlaying");
		}

		public static class PlaylistParameters {
			public static final String playlistPosition = MagicPropertyBuilder.buildMagicPropertyName(PlaylistParameters.class, "playlistPosition");
		}
	}
}
