package com.lasthopesoftware.bluewater;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;

public class BackgroundFilePreparer implements Callable<Boolean> {

	private JrFilePlayer mCurrentFilePlayer, mNextFilePlayer;
	private static final int SLEEP_TIME = 5000;
	
	public BackgroundFilePreparer(JrFilePlayer currentPlayer, JrFilePlayer nextPlayer) {
		mCurrentFilePlayer = currentPlayer;
		mNextFilePlayer = nextPlayer;
	}
	
	@Override
	public Boolean call() {
		if (mNextFilePlayer == null) return Boolean.FALSE;
		mNextFilePlayer.initMediaPlayer();
		double bufferTime = -1;
		while (mCurrentFilePlayer != null && mCurrentFilePlayer.isMediaPlayerCreated()) {
			try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException ie) {
				return Boolean.FALSE;
			}
			
			try {
				if (bufferTime < 0) {
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
			
				if (mCurrentFilePlayer.getCurrentPosition() > (mCurrentFilePlayer.getDuration() - bufferTime) && !mNextFilePlayer.isPrepared()) {
					mNextFilePlayer.prepareMpSynchronously();
					return Boolean.TRUE;
				}
			} catch (Exception e) {
				return Boolean.FALSE;
			}
		}
		return Boolean.FALSE;
	}

}