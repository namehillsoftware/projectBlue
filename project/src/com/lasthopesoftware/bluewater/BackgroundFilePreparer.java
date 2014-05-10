package com.lasthopesoftware.bluewater;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.LoggerFactory;

import android.os.AsyncTask.Status;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class BackgroundFilePreparer {

	private JrFilePlayer mCurrentFilePlayer, mNextFilePlayer;
	private static final int SLEEP_TIME = 5000;
	private SimpleTask<Void, Void, Boolean> mTask;
	
	private static ExecutorService backgroundFileService = Executors.newSingleThreadExecutor();
	
	public BackgroundFilePreparer(JrFilePlayer currentPlayer, JrFilePlayer nextPlayer) {
		mCurrentFilePlayer = currentPlayer;
		mNextFilePlayer = nextPlayer;
	}
	
	public void start() {
		mTask = new SimpleTask<Void, Void, Boolean>();
		mTask.setOnExecuteListener(new OnExecuteListener<Void, Void, Boolean>() {
			
			@Override
			public Boolean onExecute(ISimpleTask<Void, Void, Boolean> owner, Void... params) throws Exception {
				if (mNextFilePlayer == null) return Boolean.FALSE;
				mNextFilePlayer.initMediaPlayer();
				double bufferTime = -1;
				while (owner.getState() != SimpleTaskState.CANCELLED && mCurrentFilePlayer != null && mCurrentFilePlayer.isMediaPlayerCreated()) {
					try {
						if (owner.getState() != SimpleTaskState.CANCELLED) return Boolean.FALSE;
						Thread.sleep(SLEEP_TIME);
						if (owner.getState() != SimpleTaskState.CANCELLED) return Boolean.FALSE;
					} catch (InterruptedException ie) {
						return Boolean.FALSE;
					}
					
					try {
						if (owner.getState() != SimpleTaskState.CANCELLED && bufferTime < 0) {
							// figure out how much buffer time we need for this file if we're on the slowest 3G network
							// and add 15secs for a dropped connection  
							try {
								if (mNextFilePlayer.getDuration() < 0) continue;
								bufferTime = (((mNextFilePlayer.getDuration() * 128) / 384) * 1.2) + 15000;
							} catch (IOException e) {
								LoggerFactory.getLogger(BackgroundFilePreparer.class).warn(e.toString(), e);
								bufferTime = -1;
								continue;
							}
						}
					
						if (owner.getState() != SimpleTaskState.CANCELLED && mCurrentFilePlayer.getCurrentPosition() > (mCurrentFilePlayer.getDuration() - bufferTime) && !mNextFilePlayer.isPrepared()) {
							mNextFilePlayer.prepareMpSynchronously();
							return Boolean.TRUE;
						}
					} catch (Exception e) {
						return Boolean.FALSE;
					}
				}
				return Boolean.FALSE;
			}
		});
		
		mTask.executeOnExecutor(backgroundFileService);
	}
	
	public void cancel() {
		if (mTask != null) mTask.cancel(true);
	}

	public boolean isDone() {
		return mTask != null ? (mTask.getState() != SimpleTaskState.INITIALIZED && mTask.getState() != SimpleTaskState.EXECUTING)  : true;		
	}
}