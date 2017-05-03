package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.support.annotation.Nullable;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

public final class PositionedFile {
	private final int playlistPosition;
	private final ServiceFile serviceFile;

	public PositionedFile(int playlistPosition, ServiceFile serviceFile) {
		this.playlistPosition = playlistPosition;
		this.serviceFile = serviceFile;
	}

	public int getPlaylistPosition() {
		return playlistPosition;
	}

	public ServiceFile getServiceFile() {
		return serviceFile;
	}

	@Override
	public int hashCode() {
		return serviceFile.hashCode() * 31 + playlistPosition;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof PositionedFile)) return false;

		final PositionedFile other = (PositionedFile)obj;

		return
			playlistPosition == other.playlistPosition && serviceFile.equals(other.serviceFile);
	}
}
