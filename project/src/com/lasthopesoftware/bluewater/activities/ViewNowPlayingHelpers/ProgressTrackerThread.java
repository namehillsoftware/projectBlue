package com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers;

import java.io.IOException;

import android.os.Message;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;


public class ProgressTrackerThread implements Runnable {
	private JrFilePlayer mFilePlayer;
	private HandleViewNowPlayingMessages mHandler;
	
	public ProgressTrackerThread(JrFilePlayer filePlayer, HandleViewNowPlayingMessages handler) {
		mFilePlayer = filePlayer;
		mHandler = handler;
	}
	
	@Override
	public void run() {
		if (mFilePlayer != null) {
			mHandler.sendMessage(getUpdatePlayingMessage());
		}
		
		while (mFilePlayer != null) {
			try {
				
				if (mFilePlayer.isPlaying())
					mHandler.sendMessage(getUpdatePlayingMessage());
				
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
	}
	
	private Message getUpdatePlayingMessage() {
		Message msg = new Message();
		msg.what = HandleViewNowPlayingMessages.UPDATE_PLAYING;
		msg.arg1 = mFilePlayer.getCurrentPosition();
		try {
			msg.arg2 = mFilePlayer.getDuration();
		} catch (IOException e) {
			msg.what = HandleViewNowPlayingMessages.SHOW_CONNECTION_LOST;
		}
		
		return msg;
	}
}