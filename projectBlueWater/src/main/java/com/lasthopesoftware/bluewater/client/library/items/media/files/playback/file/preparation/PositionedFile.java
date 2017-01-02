package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

/**
 * Created by david on 11/6/16.
 */

public class PositionedFile {
	public final int playlistPosition;
	public final IFile file;

	public PositionedFile(int playlistPosition, IFile file) {
		this.playlistPosition = playlistPosition;
		this.file = file;
	}
}
