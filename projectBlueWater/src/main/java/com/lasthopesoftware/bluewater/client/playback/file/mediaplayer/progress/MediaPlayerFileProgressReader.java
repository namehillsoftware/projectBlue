package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerIllegalStateReporter;
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress;

import org.joda.time.Duration;

public class MediaPlayerFileProgressReader implements ReadFileProgress {

	private static final MediaPlayerIllegalStateReporter reporter = new MediaPlayerIllegalStateReporter(MediaPlayerFileProgressReader.class);

	private final MediaPlayer mediaPlayer;

	private Duration fileProgress = Duration.ZERO;

	public MediaPlayerFileProgressReader(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public synchronized Duration getProgress() {
		if (!mediaPlayer.isPlaying()) return fileProgress;

		try {
			return fileProgress = Duration.millis(mediaPlayer.getCurrentPosition());
		} catch (IllegalStateException e) {
			reporter.reportIllegalStateException(e, "reading position");
			return fileProgress;
		}
	}
}
