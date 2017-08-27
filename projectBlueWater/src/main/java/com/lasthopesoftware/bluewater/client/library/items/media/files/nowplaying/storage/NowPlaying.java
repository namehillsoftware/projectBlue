package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 1/29/17.
 */

public class NowPlaying {
	public List<ServiceFile> playlist = new ArrayList<>();
	public int playlistPosition;
	public int filePosition;
	public boolean isRepeating;

	NowPlaying(List<ServiceFile> playlist, int playlistPosition, int filePosition, boolean isRepeating) {
		this(playlistPosition, filePosition, isRepeating);

		this.playlist = playlist;
	}

	NowPlaying(int playlistPosition, int filePosition, boolean isRepeating) {
		this.playlistPosition = playlistPosition;
		this.filePosition = filePosition;
		this.isRepeating = isRepeating;
	}
}
