package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress;

public class ExoPlayerFileProgressReader implements ReadFileProgress {

	private final ExoPlayer exoPlayer;

	private FileProgress fileProgress = new FileProgress(0, 0);

	public ExoPlayerFileProgressReader(ExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
	}

	@Override
	public synchronized FileProgress getFileProgress() {
		if (!exoPlayer.getPlayWhenReady()) return fileProgress;

		return fileProgress = new FileProgress(
			exoPlayer.getCurrentPosition(),
			exoPlayer.getDuration());
	}
}
