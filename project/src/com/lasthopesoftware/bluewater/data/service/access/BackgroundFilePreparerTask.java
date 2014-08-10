package com.lasthopesoftware.bluewater.data.service.access;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class BackgroundFilePreparerTask {

	private FilePlayer mCurrentFilePlayer, mNextFilePlayer;
	private static final int SLEEP_TIME = 5000;
	private SimpleTask<Void, Void, Boolean> mTask;
	
	private static final Logger mLogger = LoggerFactory.getLogger(BackgroundFilePreparerTask.class);
	
	private static final ExecutorService backgroundFileService = Executors.newSingleThreadExecutor();
	
	public BackgroundFilePreparerTask(FilePlayer currentPlayer, FilePlayer nextPlayer) {
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
				while (!owner.isCancelled() && mCurrentFilePlayer != null && mCurrentFilePlayer.isMediaPlayerCreated()) {
					try {
						if (owner.isCancelled()) return Boolean.FALSE;
						Thread.sleep(SLEEP_TIME);
						if (owner.isCancelled()) return Boolean.FALSE;
					} catch (InterruptedException ie) {
						return Boolean.FALSE;
					}
					
					if (owner.isCancelled()) return Boolean.FALSE;
					
					try {
						if (bufferTime < 0) {
							// figure out how much buffer time we need for this file if we're on the slowest 3G network
							// and add 15secs for a dropped connection  
							try {
								if (mNextFilePlayer.getDuration() < 0) continue;
								bufferTime = (((mNextFilePlayer.getDuration() * 128) / 384) * 1.2) + 15000;
							} catch (IOException e) {
								mLogger.warn("Couldn't retrieve song duration. Trying again in " + String.valueOf(SLEEP_TIME/1000) + " seconds");
								bufferTime = -1;
								continue;
							}
						}
						
						if (owner.isCancelled()) return Boolean.FALSE;
						
						if (mCurrentFilePlayer.getCurrentPosition() > (mCurrentFilePlayer.getDuration() - bufferTime) && !mNextFilePlayer.isPrepared()) {
							mNextFilePlayer.prepareMpSynchronously();
							mLogger.info("File " + mNextFilePlayer.getFile().getValue() + " prepared");
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
		if (mTask == null) return true;
		return mTask.getState() != SimpleTaskState.INITIALIZED && mTask.getState() != SimpleTaskState.EXECUTING;
	}
}
