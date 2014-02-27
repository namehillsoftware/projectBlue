package com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers;

import java.io.IOException;

import android.os.Message;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFileMediaPlayer;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;


public class ProgressTrackerThread implements Runnable {
	private JrFileMediaPlayer mFilePlayer;
	private HandleViewNowPlayingMessages mHandler;
	
	public ProgressTrackerThread(JrFileMediaPlayer filePlayer, HandleViewNowPlayingMessages handler) {
		mFilePlayer = filePlayer;
		mHandler = handler;
	}
	
	@Override
	public void run() {
		Message msg;
		
		while (true) {
			try {
				
				msg = null;
				if (mFilePlayer !=null && mFilePlayer.isPlaying()) {
					msg = new Message();
					msg.what = HandleViewNowPlayingMessages.UPDATE_PLAYING;
					msg.arg1 = mFilePlayer.getCurrentPosition();
					try {
						msg.arg2 = mFilePlayer.getDuration();
					} catch (IOException e) {
						msg.what = HandleViewNowPlayingMessages.SHOW_CONNECTION_LOST;
					}
				}
				if (msg != null) mHandler.sendMessage(msg);
				
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
	}
}