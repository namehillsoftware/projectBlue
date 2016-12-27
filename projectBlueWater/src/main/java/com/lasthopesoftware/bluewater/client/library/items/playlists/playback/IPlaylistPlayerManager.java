package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import java.util.List;

/**
 * Created by david on 12/17/16.
 */

public interface IPlaylistPlayerManager {

	IPlaylistPlayer startAsCompletable(List<IFile> playlist, int playlistStart, int fileStart);

	IPlaylistPlayer startAsCyclical(List<IFile> playlist, int playlistStart, int fileStart);

	IPlaylistPlayer continueAsCompletable();

	IPlaylistPlayer continueAsCyclical();
}
