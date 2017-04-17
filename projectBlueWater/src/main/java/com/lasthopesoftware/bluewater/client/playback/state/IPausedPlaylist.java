package com.lasthopesoftware.bluewater.client.playback.state;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.promises.Promise;

import java.util.List;

public interface IPausedPlaylist extends IPlaylistPosition {
	Promise<IStartedPlaylist> startPlaylist(final List<ServiceFile> playlist, final int playlistPosition, final int filePosition);
	Promise<IStartedPlaylist> resume();
}
