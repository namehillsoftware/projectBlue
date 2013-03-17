/**
 * 
 */
package com.lasthopesoftware.jrmediastreamer;

import java.util.concurrent.FutureTask;

import jrAccess.JrSession;
import jrFileSystem.JrFile;
import jrFileSystem.JrListing;
import jrFileSystem.OnJrFileCompleteListener;
import jrFileSystem.OnJrFilePreparedListener;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;


/**
 * @author david
 *
 */
public class StreamingMusicService extends Service implements OnJrFilePreparedListener, OnErrorListener, 
		OnJrFileCompleteListener, OnAudioFocusChangeListener {
	
	//private final IBinder mBinder = 
	public static final String ACTION_PLAY = "com.lasthopsoftware.jrmediastreamer.ACTION_PLAY";
	private int mId = 1;
	private WifiLock mWifiLock = null;
	private String mUrl;
	private Notification mNotification;
	private NotificationManager mNotificationMgr;
	private int NOTIFICATION = R.string.streaming_music_svc_started;
	private Thread trackProgressThread;
	
	public StreamingMusicService() {
		super();
		
	}
	
	public StreamingMusicService(String url) {
		super();
		mUrl = url;
	}
	
	private void initMediaPlayers() {
		if (JrSession.playlist != null) {
			
			for (JrListing listing : JrSession.playlist.getSubItems()) {
				JrFile file = (JrFile) listing;
				file.setOnFileCompletionListener(this);
				file.setOnFilePreparedListener(this);
				initMediaPlayer(file);
			}
		}
	}

	private void initMediaPlayer(JrFile file) {
		file.initMediaPlayer(getApplicationContext());
        if (file.getUrl().equalsIgnoreCase(mUrl)) file.prepareMediaPlayer(); // prepare async to not block main thread
	}
	
	private void startMediaPlayer(JrFile file) {
		JrSession.playingFile = file;
		
		// Set the notification area
		PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, ViewNowPlaying.class), 0);
        mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "svcLock");
        mWifiLock.acquire();
		mNotification = new Notification();
//		      mNotification.tickerText = text;
		mNotification.icon = R.drawable.ic_launcher;
		mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotification.setLatestEventInfo(getApplicationContext(), 
				"Music Streamer for J. River Media Center", 
				"Playing",
				pi);
        startForeground(mId, mNotification);
        
        file.getMediaPlayer().start();
        PrepareNextMediaPlayer backgroundPreparer = new PrepareNextMediaPlayer(file);
        trackProgressThread = new Thread(backgroundPreparer);
        trackProgressThread.start();
	}
	
	private void releaseMediaPlayers() {
		for (JrListing listing : JrSession.playlist.getSubItems()) {
			JrFile file = (JrFile) listing;
			releaseMediaPlayer(file);
		}
	}
	
	private void releaseMediaPlayer(JrFile file) {
		stopForeground(true);
		mNotificationMgr.cancel(NOTIFICATION);
		mNotification = null;
		mWifiLock.release();
		mWifiLock = null;
		JrSession.playingFile = null;
	}

	/* Begin Event Handlers */
	
	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().equals(ACTION_PLAY)) {
			mUrl = intent.getDataString();
			initMediaPlayers();
        }
		return START_STICKY;
	}
	
	@Override
    public void onCreate() {
        mNotificationMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }
	
	public void onJrFilePrepared(JrFile file) {
		if (JrSession.playingFile == null && !file.getMediaPlayer().isPlaying()) startMediaPlayer(file);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		
		return mBinder;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.reset();
		return false;
	}


	@Override
	public void onJrFileComplete(JrFile file) {
		releaseMediaPlayer(file);
		if (JrSession.playlist.getSubItems().indexOf((JrListing)file) < JrSession.playlist.getSubItems().size() - 1) {
			JrFile nextFile = (JrFile)JrSession.playlist.getSubItems().get(JrSession.playlist.getSubItems().indexOf(file) + 1);
			if (!nextFile.isPrepared())
				nextFile.prepareMediaPlayer();
			else
				startMediaPlayer(nextFile);
		}
	}


	@Override
	public void onAudioFocusChange(int focusChange) {
	    switch (focusChange) {
	        case AudioManager.AUDIOFOCUS_GAIN:
	            // resume playback
	            if (JrSession.playingFile == null) initMediaPlayers();
	            else if (!JrSession.playingFile.getMediaPlayer().isPlaying()) startMediaPlayer(JrSession.playingFile);
	            JrSession.playingFile.getMediaPlayer().setVolume(1.0f, 1.0f);
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS:
	            // Lost focus for an unbounded amount of time: stop playback and release media player
	            if (JrSession.playingFile.getMediaPlayer().isPlaying()) releaseMediaPlayers();
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	            // Lost focus for a short time, but we have to stop
	            // playback. We don't release the media player because playback
	            // is likely to resume
	            if (JrSession.playingFile.getMediaPlayer().isPlaying()) JrSession.playingFile.getMediaPlayer().pause();
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	            // Lost focus for a short time, but it's ok to keep playing
	            // at an attenuated level
	            if (JrSession.playingFile.getMediaPlayer().isPlaying()) JrSession.playingFile.getMediaPlayer().setVolume(0.1f, 0.1f);
	            break;
	    }
	}
	
	@Override
	public void onDestroy() {
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
