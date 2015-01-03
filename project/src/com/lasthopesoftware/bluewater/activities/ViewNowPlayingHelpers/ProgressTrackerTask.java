package com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.AsyncTask;
import android.os.Message;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;


public class ProgressTrackerTask extends AsyncTask<Void, Void, Void> {
	private final FilePlayer mFilePlayer;
	private final HandleViewNowPlayingMessages mHandler;
	
	private static final ExecutorService mTrackerExecutor = Executors.newSingleThreadExecutor();
	
	public static ProgressTrackerTask trackProgress(FilePlayer filePlayer, HandleViewNowPlayingMessages handler) {
		ProgressTrackerTask newProgressTrackerThread = new ProgressTrackerTask(filePlayer, handler);
		newProgressTrackerThread.executeOnExecutor(mTrackerExecutor);
		return newProgressTrackerThread;
	}
	
	private ProgressTrackerTask(FilePlayer filePlayer, HandleViewNowPlayingMessages handler) {
		mFilePlayer = filePlayer;
		mHandler = handler;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		if (!isCancelled() && mFilePlayer != null) {
			mHandler.sendMessage(getUpdatePlayingMessage());
		}
		
		while (!isCancelled() && mFilePlayer != null && mFilePlayer.isMediaPlayerCreated()) {
			try {
				
				if (mFilePlayer.isPlaying())
					mHandler.sendMessage(getUpdatePlayingMessage());
				
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return null;
			}
		}
		
		return null;
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