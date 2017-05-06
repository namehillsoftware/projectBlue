package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

public final class PositionedPlaybackFile implements Comparable<PositionedPlaybackFile> {
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
	public int compareTo(@NonNull PositionedPlaybackFile other) {
		return positionedFile.compareTo(other.positionedFile);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PositionedPlaybackFile && compareTo((PositionedPlaybackFile)obj) == 0;
	}

	@Override
	public int hashCode() {
		return positionedFile.hashCode();
	}

	public PositionedFile asPositionedFile() {
		return positionedFile;
	}
}
