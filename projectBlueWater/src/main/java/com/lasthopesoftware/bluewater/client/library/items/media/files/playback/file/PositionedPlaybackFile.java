package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;

/**
 * Created by david on 11/13/16.
 */

public class PositionedPlaybackFile extends File {
	private final int position;
	private final IPlaybackHandler playbackHandler;

	public PositionedPlaybackFile(int position, IPlaybackHandler playbackHandler, File file) {
		super(file.getKey());
		this.position = position;
		this.playbackHandler = playbackHandler;
	}

	public int getPosition() {
		return position;
	}

	public IPlaybackHandler getPlaybackHandler() {
		return playbackHandler;
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 31 + position;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PositionedPlaybackFile)) return false;

		final PositionedPlaybackFile other = (PositionedPlaybackFile)obj;

		return position == other.position && super.equals(obj);
	}
}
