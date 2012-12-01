/**
 * 
 */
package com.lasthopesoftware.jrmediastreamer;

import jrAccess.JrSession;
import jrFileSystem.JrFile;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * @author david
 *
 */
public class StreamingMusicService extends Service implements
		OnPreparedListener, 
		OnErrorListener, 
		OnCompletionListener,
		OnAudioFocusChangeListener
{
	
	//private final IBinder mBinder = 
	public static final String ACTION_PLAY = "com.lasthopsoftware.jrmediastreamer.ACTION_PLAY";
	private int mId = 1;
	private WifiLock mWifiLock = null;
	private String mUrl;
	private Notification mNotification;
	private NotificationManager mNotificationMgr;
	private int NOTIFICATION = R.string.streaming_music_svc_started;
	private Intent mIntent;
	
	public StreamingMusicService() {
		super();
	}
	
	public StreamingMusicService(String url) {
		super();
		mUrl = url;
	}

	private void initMediaPlayer() {
		JrSession.mMediaPlayer = new MediaPlayer(); // initialize it here
		JrSession.mMediaPlayer.setOnPreparedListener(this);
		JrSession.mMediaPlayer.setOnErrorListener(this);
		JrSession.mMediaPlayer.setOnCompletionListener(this);
		JrSession.mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "svcLock");
        mWifiLock.acquire();
        try {
        	JrSession.mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        	JrSession.mMediaPlayer.setDataSource(mUrl);
        	JrSession.mMediaPlayer.prepareAsync(); // prepare async to not block main thread
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, ViewNowPlaying.class), 0);
		mNotification = new Notification();
//      mNotification.tickerText = text;
		mNotification.icon = R.drawable.ic_launcher;
		mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotification.setLatestEventInfo(getApplicationContext(), 
				"Music Streamer for J. River Media Center", 
				"Playing",
				pi);
        startForeground(mId, mNotification);
	}
	
	private void releaseMediaPlayer() {
		mNotificationMgr.cancel(NOTIFICATION);
		stopForeground(true);
		mWifiLock.release();
		mWifiLock = null;
		JrSession.mMediaPlayer.release();
		JrSession.mMediaPlayer = null;
	}

	/* Begin Event Handlers */
	
	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().equals(ACTION_PLAY)) {
			mUrl = intent.getDataString();
			initMediaPlayer();  
        }
		return START_STICKY;
	}
	
	@Override
    public void onCreate() {
        mNotificationMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
//        showNotification();
    }
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		JrSession.mMediaPlayer.start();
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
//		return null;
		return mBinder;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		JrSession.mMediaPlayer.reset();
		return false;
	}


	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		releaseMediaPlayer();
	}


	@Override
	public void onAudioFocusChange(int focusChange) {
	    switch (focusChange) {
	        case AudioManager.AUDIOFOCUS_GAIN:
	            // resume playback
	            if (JrSession.mMediaPlayer == null) initMediaPlayer();
	            else if (!JrSession.mMediaPlayer.isPlaying()) JrSession.mMediaPlayer.start();
	            JrSession.mMediaPlayer.setVolume(1.0f, 1.0f);
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS:
	            // Lost focus for an unbounded amount of time: stop playback and release media player
	            if (JrSession.mMediaPlayer.isPlaying()) releaseMediaPlayer();
	            JrSession.mMediaPlayer = null;
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	            // Lost focus for a short time, but we have to stop
	            // playback. We don't release the media player because playback
	            // is likely to resume
	            if (JrSession.mMediaPlayer.isPlaying()) JrSession.mMediaPlayer.pause();
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	            // Lost focus for a short time, but it's ok to keep playing
	            // at an attenuated level
	            if (JrSession.mMediaPlayer.isPlaying()) JrSession.mMediaPlayer.setVolume(0.1f, 0.1f);
	            break;
	    }
	}
	
	@Override
	public void onDestroy() {
		releaseMediaPlayer();
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
