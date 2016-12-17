package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import java.util.List;

/**
 * Created by david on 12/17/16.
 */

public interface IPlaylistPlayerProducer {
	IPlaylistPlayer getPlaylistPlayer(List<IFile> files, int startFilePosition, int startFileAt, boolean isCyclical);
}
