/**
 * 
 */
package com.lasthopesoftware.jrmediastreamer;


import jrAccess.JrSession;
import jrFileSystem.JrFile;
import jrFileSystem.OnJrFileCompleteListener;
import jrFileSystem.OnJrFilePreparedListener;
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
import android.support.v4.app.NotificationCompat;


/**
 * @author david
 *
 */
public class StreamingMusicService extends Service implements OnJrFilePreparedListener, OnErrorListener, 
		OnJrFileCompleteListener, OnAudioFocusChangeListener {
	
	//private final IBinder mBinder = 
	public static final String ACTION_PLAY = "com.lasthopsoftware.jrmediastreamer.ACTION_PLAY";
	public static final String ACTION_STOP = "com.lasthopsoftware.jrmediastreamer.ACTION_STOP";
	private int mId = 1;
	private WifiLock mWifiLock = null;
	private String mUrl;
	private int NOTIFICATION = R.string.streaming_music_svc_started;
	private NotificationManager mNotificationMgr;
	private Thread trackProgressThread;
	AudioManager mAudioManager;
	
	public StreamingMusicService() {
		super();
		
	}
	
	public StreamingMusicService(String url) {
		super();
		mUrl = url;
	}
	
	private void initMediaPlayers() {
		if (JrSession.playlist != null) {
			
			for (JrFile file : JrSession.playlist) {
				file.setOnFileCompletionListener(this);
				file.setOnFilePreparedListener(this);
				if (file.getUrl().equalsIgnoreCase(mUrl)) {
		        	file.initMediaPlayer(this);
		        	file.prepareMediaPlayer(); // prepare async to not block main thread
		        }
			}
		}
	}

	private void startMediaPlayer(JrFile file) {
		JrSession.playingFile = file;
		mUrl = file.getUrl();
		// Set the notification area
		PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, ViewNowPlaying.class), 0);
        mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "svcLock");
        mWifiLock.acquire();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setOngoing(true);
		builder.setContentTitle("Music Streamer Now Playing");
		builder.setContentText(file.getArtist() + " - " + file.getValue());
		builder.setContentIntent(pi);
		mNotificationMgr.notify(mId, builder.build());        
        
        file.getMediaPlayer().start();
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        PrepareNextMediaPlayer backgroundPreparer = new PrepareNextMediaPlayer(this, file);
        if (file.getNextFile() != null) {
	        trackProgressThread = new Thread(backgroundPreparer);
	        trackProgressThread.setName("Thread to prepare file " + file.getNextFile().getValue());
	        trackProgressThread.setPriority(Thread.MIN_PRIORITY);
	        trackProgressThread.start();
        }
	}
	
	private void releaseMediaPlayers() {
		for (JrFile file : JrSession.playlist) {
			releaseMediaPlayer(file);
		}
	}
	
	private void releaseMediaPlayer(JrFile file) {
		JrSession.playingFile = null;
	}

	/* Begin Event Handlers */
	
	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().equals(ACTION_PLAY)) {
			mUrl = intent.getDataString();
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
	        builder.setSmallIcon(R.drawable.ic_launcher);
			builder.setOngoing(true);
			builder.setContentTitle("Starting Music Streamer");
	        startForeground(mId, builder.build());
	        
			initMediaPlayers();
        }
		return START_STICKY;
	}
	
	@Override
    public void onCreate() {
		mNotificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
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
		mAudioManager.abandonAudioFocus(this);
		mWifiLock.release();
		mWifiLock = null;
		releaseMediaPlayer(file);
		if (file.getNextFile() != null) {
			JrFile nextFile = file.getNextFile();
			if (!nextFile.isPrepared())
				nextFile.prepareMediaPlayer();
			else
				startMediaPlayer(nextFile);
			return;
		}
		stopForeground(true);
		mNotificationMgr.cancel(mId);
	}


	@Override
	public void onAudioFocusChange(int focusChange) {
	    switch (focusChange) {
	        case AudioManager.AUDIOFOCUS_GAIN:
	            // resume playback
	            if (JrSession.playlist == null || JrSession.playlist.isEmpty()) initMediaPlayers();
	            else if (JrSession.playingFile.getMediaPlayer() == null) startMediaPlayer(JrSession.playingFile);
	            else if (!JrSession.playingFile.getMediaPlayer().isPlaying()) JrSession.playingFile.getMediaPlayer().start();
	            JrSession.playingFile.getMediaPlayer().setVolume(1.0f, 1.0f);
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS:
	            // Lost focus for an unbounded amount of time: stop playback and release media player
	            if (JrSession.playingFile.getMediaPlayer().isPlaying()) {
	            	JrSession.playlist.clear();
	            	JrSession.playingFile.getMediaPlayer().stop();
	            	releaseMediaPlayers();
	            }
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
		stopForeground(true);
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
