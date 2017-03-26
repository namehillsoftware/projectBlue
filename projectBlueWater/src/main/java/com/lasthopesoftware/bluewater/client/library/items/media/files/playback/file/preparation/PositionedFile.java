package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.support.annotation.Nullable;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

/**
 * Created by david on 11/6/16.
 */

public class PositionedFile {
	public final int playlistPosition;
	public final ServiceFile serviceFile;

	public PositionedFile(int playlistPosition, ServiceFile serviceFile) {
		this.playlistPosition = playlistPosition;
		this.serviceFile = serviceFile;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof PositionedFile)) return false;

		final PositionedFile other = (PositionedFile)obj;

		return
			playlistPosition == other.playlistPosition &&
			serviceFile.equals(other.serviceFile);
	}
}
