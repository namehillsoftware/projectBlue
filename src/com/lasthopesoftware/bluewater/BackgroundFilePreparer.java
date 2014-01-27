package com.lasthopesoftware.bluewater;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import android.content.Context;

import com.lasthopesoftware.bluewater.data.service.objects.JrFile;

public class BackgroundFilePreparer implements Runnable {

	private JrFile mCurrentFile;
	private JrFile mNextFile;
	private Context mContext;
	private static final int SLEEP_TIME = 5000;
	
	public BackgroundFilePreparer(Context context, JrFile currentFile) {
		mCurrentFile = currentFile;
		if (mCurrentFile.getNextFile() == null) return;
		mContext = context;
		mNextFile = mCurrentFile.getNextFile();
		
	}
	
	@Override
	public void run() {
		if (mNextFile == null) return;
		mNextFile.initMediaPlayer(mContext);
		double bufferTime = -1;
		while (mCurrentFile != null && mCurrentFile.isMediaPlayerCreated()) {
			try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException ie) {
				return;
			}
			
			if (bufferTime < 0) {
				// figure out how much buffer time we need for this file if we're on the slowest 3G network
				// and add 15secs for a dropped connection  
				try {
					if (mNextFile.getDuration() < 0) continue;
					bufferTime = (((mNextFile.getDuration() * 128) / 384) * 1.2) + 15000;
				} catch (IOException e) {
					LoggerFactory.getLogger(BackgroundFilePreparer.class).error(e.toString(), e);
					bufferTime = -1;
					continue;
				}
			}
			try {
				if (mCurrentFile.getCurrentPosition() > (mCurrentFile.getDuration() - bufferTime) && !mNextFile.isPrepared()) {
					mNextFile.prepareMpSynchronously();
				}
			} catch (Exception e) {
				return;
			}
		}
	}

}
