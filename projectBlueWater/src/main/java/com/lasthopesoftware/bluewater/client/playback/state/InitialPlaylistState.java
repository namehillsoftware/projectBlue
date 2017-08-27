package com.lasthopesoftware.bluewater.client.playback.state;


import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

import java.util.List;

public class InitialPlaylistState {
	public final List<ServiceFile> playlist;
	public final int playlistPosition;
	public final int filePosition;
	public final boolean isRepeating;
	public final float volume;

	public InitialPlaylistState(List<ServiceFile> playlist, int playlistPosition, int filePosition, boolean isRepeating, float volume) {
		this.playlist = playlist;
		this.playlistPosition = playlistPosition;
		this.filePosition = filePosition;
		this.isRepeating = isRepeating;
		this.volume = volume;
	}
}
