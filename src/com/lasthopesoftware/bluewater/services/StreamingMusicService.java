/**
 * 
 */
package com.lasthopesoftware.bluewater.services;


import java.io.IOException;
import java.util.ArrayList;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.bluewater.BackgroundFilePreparer;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewNowPlaying;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.data.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.objects.JrFile;
import com.lasthopesoftware.bluewater.data.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.objects.JrSession;
import com.lasthopesoftware.bluewater.data.objects.OnJrFileCompleteListener;
import com.lasthopesoftware.bluewater.data.objects.OnJrFileErrorListener;
import com.lasthopesoftware.bluewater.data.objects.OnJrFilePreparedListener;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;


/**
 * @author david
 *
 */
public class StreamingMusicService extends Service implements OnJrFilePreparedListener, OnJrFileErrorListener, 
		OnJrFileCompleteListener, OnAudioFocusChangeListener {
	
	//private final IBinder mBinder = 
	public static final String ACTION_START = "com.lasthopesoftware.bluewater.ACTION_START";
	public static final String ACTION_PLAY = "com.lasthopesoftware.bluewater.ACTION_PLAY";
	public static final String ACTION_STOP = "com.lasthopesoftware.bluewater.ACTION_STOP";
	public static final String ACTION_PAUSE = "com.lasthopesoftware.bluewater.ACTION_PAUSE";
	public static final String ACTION_SYSTEM_PAUSE = "com.lasthopesoftware.bluewater.ACTION_SYSTEM_PAUSE";
	public static final String ACTION_STOP_WAITING_FOR_CONNECTION = "com.lasthopesoftware.bluewater.ACTION_STOP_WAITING_FOR_CONNECTION";
	
	private static final String BAG_FILE_KEY = "com.lasthopesoftware.bluewater.bag.FILE_KEY";
	private static final String BAG_PLAYLIST = "com.lasthopesoftware.bluewater.bag.FILE_PLAYLIST";
	private static final String BAG_START_POS = "com.lasthopesoftware.bluewater.bag.START_POS";
	
	private static int mId = 42;
	private WifiLock mWifiLock = null;
//	private String mUrl;
	private int mFileKey = -1;
	private int mStartPos = 0;
	private NotificationManager mNotificationMgr;
	private Thread trackProgressThread;
	private static ArrayList<JrFile> mPlaylist;
	private static String mPlaylistString;
	private Context thisContext;
	private AudioManager mAudioManager;
	
	public static void StreamMusic(Context context, int startFileKey, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, startFileKey);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
		ViewUtils.CreateNowPlayingView(context);
	}
	
	public static void StreamMusic(Context context, int startFileKey, int startPos, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, startFileKey);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		svcIntent.putExtra(BAG_START_POS, startPos);
		context.startService(svcIntent);
	}
	
	public static void StreamMusic(Context context, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
		ViewUtils.CreateNowPlayingView(context);
	}
	
	public static void Play(Context context) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_PLAY);
		context.startService(svcIntent);
	}
	
	public static void Pause(Context context) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_PAUSE);
		context.startService(svcIntent);
	}
	
	public static void Next(Context context) {
		JrFile nextFile = JrSession.PlayingFile.getNextFile();
		if (nextFile == null) return;
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, nextFile.getKey());
		svcIntent.putExtra(BAG_PLAYLIST, mPlaylistString);
		context.startService(svcIntent);
	}
	
	public static void Previous(Context context) {
		JrFile previousFile = JrSession.PlayingFile.getPreviousFile();
		if (previousFile == null) return;
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, previousFile.getKey());
		svcIntent.putExtra(BAG_PLAYLIST, mPlaylistString);
		context.startService(svcIntent);
	}
	
	public StreamingMusicService() {
		super();
		thisContext = this;
	}

	private void startMediaPlayer(JrFile file) {
		JrSession.PlayingFile = file;
		mFileKey = file.getKey();
		// Set the notification area
		Intent viewIntent = new Intent(this, ViewNowPlaying.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, viewIntent, 0);
        mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "svcLock");
        mWifiLock.acquire();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		builder.setOngoing(true);
		builder.setContentTitle("Music Streamer Now Playing");
		try {
			builder.setContentText(file.getProperty("Artist") + " - " + file.getValue());
		} catch (IOException e) {
			builder.setContentText("Error getting file properties.");
		}
		builder.setContentIntent(pi);
		mNotificationMgr.notify(mId, builder.build());        
        
        file.start();
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        BackgroundFilePreparer backgroundProgressThread = new BackgroundFilePreparer(this, file);
        if (file.getNextFile() != null) {
        	if (trackProgressThread != null && trackProgressThread.isAlive()) trackProgressThread.interrupt();
	        trackProgressThread = new Thread(backgroundProgressThread);
	        trackProgressThread.setName("Thread to prepare file " + file.getNextFile().getValue());
	        trackProgressThread.setPriority(Thread.MIN_PRIORITY);
	        trackProgressThread.start();
        }
	}
	
	private void stopPlayback(boolean isUserInterrupted) {
		
		if (JrSession.PlayingFile != null) {
			if (JrSession.PlayingFile.isPlaying()) {
				if (isUserInterrupted) mAudioManager.abandonAudioFocus(this);
				JrSession.PlayingFile.stop();
			}
			
			JrSession.SaveSession(this);
			JrSession.PlayingFile = null;
		}
		
		releaseMediaPlayers();
		stopNotification();
	}
	
	private void pausePlayback(boolean isUserInterrupted) {
		if (JrSession.PlayingFile != null) {
			if (JrSession.PlayingFile.isPlaying()) {
				if (isUserInterrupted) mAudioManager.abandonAudioFocus(this);
				JrSession.PlayingFile.pause();
			}
			
			JrSession.SaveSession(this);
		}
		mPlaylistString = null;
		mFileKey = -1;
		releaseMediaPlayers();
		stopNotification();
	}
	
	private void stopNotification() {
		stopForeground(true);
		mNotificationMgr.cancel(mId);
	}
	
	private void releaseMediaPlayer(JrFile file) {
		file.releaseMediaPlayer();
		file.removeOnJrFileCompleteListener(this);
		file.removeOnJrFileErrorListener(this);
		file.removeOnJrFilePreparedListener(this);
	}
	
	private void releaseMediaPlayers() {
		for (JrFile file : mPlaylist) releaseMediaPlayer(file);
	}

	/* Begin Event Handlers */
	
	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		/* Should be modified to save its state locally in the future.
		 */
		 
		if (intent != null) {
			// 3/5 times it's going to be this so let's see if we can get
			// some improved prefetching by the processor
			if (intent.getAction().equals(ACTION_START)) {
				initializePlaylist(intent.getStringExtra(BAG_PLAYLIST), intent.getIntExtra(BAG_FILE_KEY, -1), intent.getIntExtra(BAG_START_POS, -1));
	        } else if (mPlaylist != null && JrSession.PlayingFile != null) {
	        	// These actions can only occur if mPlaylist and the PlayingFile are not null
	        	if (intent.getAction().equals(ACTION_PAUSE)) {
	        		pausePlayback(true);
		        } else if (intent.getAction().equals(ACTION_PLAY) && JrSession.PlayingFile != null) {
		    		if (!JrSession.PlayingFile.isMediaPlayerCreated()) initializePlaylist(mPlaylistString, JrSession.PlayingFile.getKey(), JrSession.PlayingFile.getCurrentPosition());
		    		else startMediaPlayer(JrSession.PlayingFile);
		        } else if (intent.getAction().equals(ACTION_STOP)) {
		        	stopPlayback(true);
		        }
	        } 
	        else if (intent.getAction().equals(ACTION_STOP_WAITING_FOR_CONNECTION)) {
	        	PollConnectionTask.Instance.get().stopPolling();
	        }
		} else if (!JrSession.Active) {
			if (JrSession.CreateSession(this)) pausePlayback(true);
		}
		return START_NOT_STICKY;
	}
	
	private void initializePlaylist(String playlistString, int fileKey) {
		initializePlaylist(playlistString, fileKey, 0);
	}
	
	private void initializePlaylist(String playlistString, int fileKey, int filePos) {
		// If everything is the same as before, and stuff is playing, don't do anything else 
		if (mPlaylistString != null && mPlaylistString.equals(playlistString) && mFileKey == fileKey && JrSession.PlayingFile.isPlaying()) return;
		
		// If the playlist has changed, change that
		if (!playlistString.equals(mPlaylistString)) {
			mPlaylistString = playlistString;
			JrSession.Playlist = mPlaylistString;
			mPlaylist = JrFiles.deserializeFileStringList(mPlaylistString);
		}

		// stop any playback that is in action
		if (JrSession.PlayingFile != null) stopPlayback(false);
		
		mFileKey = fileKey < 0 ? mPlaylist.get(0).getKey() : fileKey;
		mStartPos = filePos < 0 ? 0 : filePos;
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		builder.setOngoing(true);
		builder.setContentTitle("Starting Music Streamer");
        startForeground(mId, builder.build());
        
        for (JrFile file : mPlaylist) {
			if (file.getKey() != mFileKey) continue;
			
			file.addOnJrFileCompleteListener(this);
			file.addOnJrFilePreparedListener(this);
			file.addOnJrFileErrorListener(this);
        	file.initMediaPlayer(this);
        	file.seekTo(mStartPos);
        	file.prepareMediaPlayer(); // prepare async to not block main thread
        	break;
		}
	}
	
	@Override
    public void onCreate() {
		mNotificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	}
	
	public void onJrFilePrepared(JrFile file) {
		if (JrSession.PlayingFile == null && !file.isPlaying()) startMediaPlayer(file);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onJrFileError(JrFile file, int what, int extra) {
		NotificationCompat.Builder builder;
		
		JrSession.PlayingFile = file;
		JrSession.Playlist = mPlaylistString;
		JrSession.SaveSession(this);
		stopPlayback(true);
		
		switch (what) {
			case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
				builder = new NotificationCompat.Builder(this);
		        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
				builder.setOngoing(true);
				// Add intent for canceling waiting for connection to come back
				Intent intent = new Intent(ACTION_STOP_WAITING_FOR_CONNECTION);
				PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				builder.setContentIntent(pi);
				builder.setContentTitle("Waiting for Connection.");
				builder.setTicker("Waiting for Connection.");
				builder.setSubText("Click here to cancel.");
				mNotificationMgr.notify(mId, builder.build());
				PollConnectionTask checkConnection = PollConnectionTask.Instance.get();
				
				checkConnection.addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
					
					@Override
					public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
						mNotificationMgr.cancelAll();
						if (result == Boolean.TRUE && JrSession.CreateSession(thisContext))
							StreamMusic(thisContext, JrSession.PlayingFile.getKey(), JrSession.PlayingFile.getCurrentPosition(), JrSession.Playlist);							
					}
				});
				
				checkConnection.startPolling();
				
				break;
			default:
				builder = new NotificationCompat.Builder(this);
		        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
				builder.setOngoing(false);
				builder.setContentTitle("An error has occurred during playback.");
				break;
		}
		return true;
	}


	@Override
	public void onJrFileComplete(JrFile file) {
		mAudioManager.abandonAudioFocus(this);
		// release the wifilock if we still have it
		if (mWifiLock != null) {
			if (mWifiLock.isHeld()) mWifiLock.release();
			mWifiLock = null;
		}
		JrFile nextFile = file.getNextFile();
		releaseMediaPlayer(file);
		if (nextFile != null) {
			nextFile.addOnJrFileCompleteListener(this);
			nextFile.addOnJrFileErrorListener(this);
			if (!nextFile.isPrepared()) {
				nextFile.addOnJrFilePreparedListener(this);
				nextFile.prepareMediaPlayer();
			} else {
				startMediaPlayer(nextFile);
			}
			return;
		}
	}


	@Override
	public void onAudioFocusChange(int focusChange) {
	    switch (focusChange) {
	        case AudioManager.AUDIOFOCUS_GAIN:
	            // resume playback
	            /*if (Playlist == null || Playlist.isEmpty()) initMediaPlayers();
	            else */
	        	if (!JrSession.PlayingFile.isMediaPlayerCreated()) {
	        		initializePlaylist(JrSession.Playlist, JrSession.PlayingFile.getKey());
	        	} else if (!JrSession.PlayingFile.isPlaying()) {
	            	startMediaPlayer(JrSession.PlayingFile);
	            }
	            JrSession.PlayingFile.setVolume(1.0f);
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS:
	            // Lost focus for an unbounded amount of time: stop playback and release media player
	            if (JrSession.PlayingFile.isPlaying()) stopPlayback(false);
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	            // Lost focus for a short time, but we have to stop
	            // playback. We don't release the media player because playback
	            // is likely to resume
	            if (JrSession.PlayingFile.isPlaying()) pausePlayback(false);
	            break;
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	            // Lost focus for a short time, but it's ok to keep playing
	            // at an attenuated level
	            if (JrSession.PlayingFile.isPlaying()) JrSession.PlayingFile.setVolume(0.1f);
	            break;
	    }
	}
	
	@Override
	public void onDestroy() {
		JrSession.SaveSession(this);
		stopNotification();
		if (trackProgressThread != null && trackProgressThread.isAlive()) trackProgressThread.interrupt();
		releaseMediaPlayers();
	}

	/* End Event Handlers */

	/* Begin Binder Code */
	
	public class StreamingMusicServiceBinder extends Binder {
        StreamingMusicService getService() {
            return StreamingMusicService.this;
        }
    }

    private final IBinder mBinder = new StreamingMusicServiceBinder();
	/* End Binder Code */
}
