package com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers;

import java.io.IOException;

import android.os.Message;

import com.lasthopesoftware.bluewater.data.service.objects.JrFile;


public class ProgressTrackerThread implements Runnable {
	private JrFile mFile;
	private HandleViewNowPlayingMessages mHandler;
	
	public ProgressTrackerThread(JrFile file, HandleViewNowPlayingMessages handler) {
		mFile = file;
		mHandler = handler;
	}
	
	@Override
	public void run() {
		Message msg;
		
		while (true) {
			try {
				
				msg = null;
				if (mFile !=null && mFile.isPlaying()) {
					msg = new Message();
					msg.what = HandleViewNowPlayingMessages.UPDATE_PLAYING;
					msg.arg1 = mFile.getCurrentPosition();
					try {
						msg.arg2 = mFile.getDuration();
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