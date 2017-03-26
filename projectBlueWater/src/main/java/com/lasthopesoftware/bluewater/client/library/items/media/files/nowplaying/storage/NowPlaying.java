package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 1/29/17.
 */

public class NowPlaying {
	public List<File> playlist = new ArrayList<>();
	public int playlistPosition;
	public int filePosition;
	public boolean isRepeating;

	NowPlaying(List<File> playlist, int playlistPosition, int filePosition, boolean isRepeating) {
		this(playlistPosition, filePosition, isRepeating);

		this.playlist = playlist;
	}

	NowPlaying(int playlistPosition, int filePosition, boolean isRepeating) {
		this.playlistPosition = playlistPosition;
		this.filePosition = filePosition;
		this.isRepeating = isRepeating;
	}
}
