package com.lasthopesoftware.jrmediastreamer;

import jrAccess.JrSession;
import jrFileSystem.JrFile;

public class PrepareNextMediaPlayer implements Runnable {

	private JrFile mCurrentFile;
	private JrFile mNextFile;
	private double mBufferTime;
	
	public PrepareNextMediaPlayer(JrFile currentFile) {
		mCurrentFile = currentFile;
		int nextFilePosition = JrSession.playlist.getSubItems().indexOf(mCurrentFile) + 1;
		if (nextFilePosition >= JrSession.playlist.getSubItems().size()) return;
		mNextFile = (JrFile)JrSession.playlist.getSubItems().get(nextFilePosition);
		// figure out how much buffer time we need for this file if we're on the slowest 3G network
		mBufferTime = ((mNextFile.getDuration() * 128) / 384) * 1.2; 
	}
	
	@Override
	public void run() {
		if (mNextFile == null) return;
		while (mCurrentFile != null && mCurrentFile.getMediaPlayer() != null && mCurrentFile.getMediaPlayer().isPlaying()) {
			try {
				if (mCurrentFile.getMediaPlayer().getCurrentPosition() > (mCurrentFile.getMediaPlayer().getDuration() - mBufferTime)) {
					if (!mNextFile.isPrepared()) {
						mNextFile.prepareMediaPlayer();
						return;
					}
				}
				Thread.sleep(5000);
			} catch (Exception e) {
				return;
			}
		}

	}

}
