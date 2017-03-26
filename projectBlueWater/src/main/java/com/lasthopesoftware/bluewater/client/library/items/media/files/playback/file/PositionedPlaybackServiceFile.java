package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

/**
 * Created by david on 11/13/16.
 */

public class PositionedPlaybackServiceFile extends ServiceFile {
	private final int position;
	private final IPlaybackHandler playbackHandler;

	public PositionedPlaybackServiceFile(int position, IPlaybackHandler playbackHandler, ServiceFile serviceFile) {
		super(serviceFile.getKey());
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
		if (!(obj instanceof PositionedPlaybackServiceFile)) return false;

		final PositionedPlaybackServiceFile other = (PositionedPlaybackServiceFile)obj;

		return position == other.position && super.equals(obj);
	}
}
