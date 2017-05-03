package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

/**
 * Created by david on 11/13/16.
 */

public final class PositionedPlaybackFile {
	private final IPlaybackHandler playbackHandler;
	private final PositionedFile positionedFile;

	public PositionedPlaybackFile(int playlistPosition, IPlaybackHandler playbackHandler, ServiceFile serviceFile) {
		this(playbackHandler, new PositionedFile(playlistPosition, serviceFile));
	}

	public PositionedPlaybackFile(IPlaybackHandler playbackHandler, PositionedFile positionedFile) {
		this.positionedFile = positionedFile;
		this.playbackHandler = playbackHandler;
	}

	public int getPlaylistPosition() {
		return positionedFile.getPlaylistPosition();
	}

	public ServiceFile getServiceFile() {
		return positionedFile.getServiceFile();
	}

	public IPlaybackHandler getPlaybackHandler() {
		return playbackHandler;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PositionedPlaybackFile)) return false;

		final PositionedPlaybackFile other = (PositionedPlaybackFile)obj;

		return positionedFile.equals(other.positionedFile);
	}

	@Override
	public int hashCode() {
		return positionedFile.hashCode();
	}

	public PositionedFile asPositionedFile() {
		return positionedFile;
	}
}
