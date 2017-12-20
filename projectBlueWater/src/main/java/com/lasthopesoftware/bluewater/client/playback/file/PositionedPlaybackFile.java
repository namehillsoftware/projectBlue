package com.lasthopesoftware.bluewater.client.playback.file;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

public final class PositionedPlaybackFile implements Comparable<PositionedPlaybackFile> {
	private final PlayableFile playbackHandler;
	private final PositionedFile positionedFile;

	public PositionedPlaybackFile(int playlistPosition, PlayableFile playbackHandler, ServiceFile serviceFile) {
		this(playbackHandler, new PositionedFile(playlistPosition, serviceFile));
	}

	public PositionedPlaybackFile(PlayableFile playbackHandler, PositionedFile positionedFile) {
		this.positionedFile = positionedFile;
		this.playbackHandler = playbackHandler;
	}

	public int getPlaylistPosition() {
		return positionedFile.getPlaylistPosition();
	}

	public ServiceFile getServiceFile() {
		return positionedFile.getServiceFile();
	}

	public PlayableFile getPlaybackHandler() {
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
