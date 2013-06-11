package com.lasthopesoftware.jrmediastreamer;

import android.content.Context;
import jrFileSystem.JrFile;

public class BackgroundFilePreparer implements Runnable {

	private JrFile mCurrentFile;
	private JrFile mNextFile;
	private double mBufferTime;
	private Context mContext;
	
	public BackgroundFilePreparer(Context context, JrFile currentFile) {
		mCurrentFile = currentFile;
		if (mCurrentFile.getNextFile() == null) return;
		mContext = context;
		mNextFile = mCurrentFile.getNextFile();
		// figure out how much buffer time we need for this file if we're on the slowest 3G network
		// and add 15secs for a dropped connection 
		mBufferTime = (((Double.parseDouble(mNextFile.getProperty("Duration")) * 1000 * 128) / 384) * 1.2) + 15000; 
	}
	
	@Override
	public void run() {
		if (mNextFile == null) return;
		mNextFile.initMediaPlayer(mContext);
		while (mCurrentFile != null && mCurrentFile.getMediaPlayer() != null) {
			try {
				if (mCurrentFile.getMediaPlayer().getCurrentPosition() > (mCurrentFile.getMediaPlayer().getDuration() - mBufferTime)) {
					if (!mNextFile.isPrepared()) mNextFile.prepareMpSynchronously();
				}
				Thread.sleep(5000);
			} catch (Exception e) {
				return;
			}
		}
	}

}
