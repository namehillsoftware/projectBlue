package com.lasthopesoftware.jrmediastreamer;

import java.util.Date;

import android.content.Context;
import android.text.method.DateTimeKeyListener;
import jrFileSystem.JrFile;

public class BackgroundFileWorker implements Runnable {

	private JrFile mCurrentFile;
	private JrFile mNextFile;
	private double mBufferTime;
	private Context mContext;
	
	public BackgroundFileWorker(Context context, JrFile currentFile) {
		mCurrentFile = currentFile;
		if (mCurrentFile.getNextFile() == null) return;
		mContext = context;
		mNextFile = mCurrentFile.getNextFile();
		// figure out how much buffer time we need for this file if we're on the slowest 3G network
		mBufferTime = ((Double.parseDouble(mNextFile.getProperty("Duration")) * 128) / 384) * 1.2 + 15000; 
	}
	
	@Override
	public void run() {
		if (mNextFile == null) return;
		mNextFile.initMediaPlayer(mContext);
		boolean isPropertiesUpdated = false;
		int duration = mCurrentFile.getMediaPlayer().getDuration();
		while (mCurrentFile != null && mCurrentFile.getMediaPlayer() != null) {
			try {
				int currentPosition = mCurrentFile.getMediaPlayer().getCurrentPosition();
				if (currentPosition > (duration - mBufferTime) && !mNextFile.isPrepared()) {
					mNextFile.prepareMediaPlayer();
				}
				currentPosition = mCurrentFile.getMediaPlayer().getCurrentPosition();
				if (currentPosition > (duration - 10000) && !isPropertiesUpdated) {
					int numberPlays = Integer.parseInt(mCurrentFile.getProperty("Number Plays"));
					mCurrentFile.setProperty("Number Plays", String.valueOf(++numberPlays));
					
					String lastPlayed = String.valueOf(System.currentTimeMillis()/1000);
					mCurrentFile.setProperty("Last Played", lastPlayed);
					isPropertiesUpdated = true;
				}
				Thread.sleep(1000);
			} catch (Exception e) {
				return;
			}
		}
	}

}
