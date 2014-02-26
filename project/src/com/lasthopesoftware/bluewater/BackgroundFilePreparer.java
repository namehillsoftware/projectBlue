package com.lasthopesoftware.bluewater;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFileMediaPlayer;

public class BackgroundFilePreparer implements Runnable {

	private JrFileMediaPlayer mCurrentFilePlayer, mNextFilePlayer;
	private static final int SLEEP_TIME = 5000;
	
	public BackgroundFilePreparer(JrFileMediaPlayer currentPlayer, JrFileMediaPlayer nextPlayer) {
		mCurrentFilePlayer = currentPlayer;
		mNextFilePlayer = nextPlayer;
	}
	
	@Override
	public void run() {
		if (mNextFilePlayer == null) return;
		mCurrentFilePlayer.initMediaPlayer();
		double bufferTime = -1;
		while (mCurrentFilePlayer != null && mCurrentFilePlayer.isMediaPlayerCreated()) {
			try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException ie) {
				return;
			}
			
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
			try {
				if (mCurrentFilePlayer.getCurrentPosition() > (mCurrentFilePlayer.getDuration() - bufferTime) && !mNextFilePlayer.isPrepared()) {
					mNextFilePlayer.prepareMpSynchronously();
				}
			} catch (Exception e) {
				return;
			}
		}
	}

}