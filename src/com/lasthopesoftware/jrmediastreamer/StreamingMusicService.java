/**
 * 
 */
package com.lasthopesoftware.jrmediastreamer;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;

/**
 * @author david
 *
 */
public class StreamingMusicService extends Service implements
		OnPreparedListener, OnErrorListener {
	private static final String ACTION_PLAY = "com.example.action.PLAY";
	private MediaPlayer mMediaPlayer = null;
	private String mUrl;
	
	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		mMediaPlayer.start();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().equals(ACTION_PLAY)) {
            mMediaPlayer = new MediaPlayer(); // initialize it here
            mMediaPlayer.setOnPreparedListener(this);
            try {
            	mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mMediaPlayer.setDataSource(mUrl);
				mMediaPlayer.prepareAsync(); // prepare async to not block main thread
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
        }
		return START_STICKY;
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	}

}
