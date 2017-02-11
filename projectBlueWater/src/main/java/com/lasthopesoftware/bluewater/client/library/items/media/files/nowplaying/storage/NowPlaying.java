package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import java.util.List;

/**
 * Created by david on 1/29/17.
 */

public class NowPlaying {
	public List<IFile> playlist;
	public int playlistPosition;
	public int filePosition;
	public boolean isRepeating;

	NowPlaying(List<IFile> playlist, int playlistPosition, int filePosition, boolean isRepeating) {
		this.playlist = playlist;
		this.playlistPosition = playlistPosition;
		this.filePosition = filePosition;
		this.isRepeating = isRepeating;
	}
}
