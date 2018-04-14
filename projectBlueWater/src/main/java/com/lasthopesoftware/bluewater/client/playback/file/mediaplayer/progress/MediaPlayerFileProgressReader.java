package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerIllegalStateReporter;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress;

public class MediaPlayerFileProgressReader implements ReadFileProgress {

	private static final MediaPlayerIllegalStateReporter reporter = new MediaPlayerIllegalStateReporter(MediaPlayerFileProgressReader.class);

	private final MediaPlayer mediaPlayer;

	private FileProgress fileProgress = new FileProgress(0, 0);

	public MediaPlayerFileProgressReader(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public synchronized FileProgress getFileProgress() {
		if (!mediaPlayer.isPlaying()) return fileProgress;

		final long position;
		try {
			position = mediaPlayer.getCurrentPosition();
		} catch (IllegalStateException e) {
			reporter.reportIllegalStateException(e, "reading position");
			return fileProgress;
		}

		final long duration;
		try {
			duration = mediaPlayer.getDuration();
		} catch (IllegalStateException e) {
			reporter.reportIllegalStateException(e, "reading duration");
			return fileProgress;
		}

		return fileProgress = new FileProgress(position, duration);
	}
}
