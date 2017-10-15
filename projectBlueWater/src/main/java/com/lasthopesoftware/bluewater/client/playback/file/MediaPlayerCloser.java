package com.lasthopesoftware.bluewater.client.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerIllegalStateReporter;

public class MediaPlayerCloser {

	private static final MediaPlayerIllegalStateReporter mediaPlayerIllegalStateReporter = new MediaPlayerIllegalStateReporter(MediaPlayerCloser.class);

	public static void closeMediaPlayer(MediaPlayer mediaPlayer) {
		try {
			mediaPlayer.reset();
		} catch (IllegalStateException se) {
			mediaPlayerIllegalStateReporter.reportIllegalStateException(se, "resetting");
		}

		try {
			mediaPlayer.release();
		} catch (IllegalStateException se) {
			mediaPlayerIllegalStateReporter.reportIllegalStateException(se, "releasing");
		}
	}
}
