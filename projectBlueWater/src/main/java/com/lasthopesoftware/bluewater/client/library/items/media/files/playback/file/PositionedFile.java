package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.support.annotation.Nullable;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

/**
 * Created by david on 11/6/16.
 */

public class PositionedFile extends ServiceFile {
	private final int playlistPosition;

	public PositionedFile(int playlistPosition, ServiceFile serviceFile) {
		super(serviceFile.getKey());

		this.playlistPosition = playlistPosition;
	}

	public int getPlaylistPosition() {
		return playlistPosition;
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 31 + playlistPosition;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof PositionedFile)) return false;

		final PositionedFile other = (PositionedFile)obj;

		return
			playlistPosition == other.playlistPosition &&
			super.equals(obj);
	}
}
