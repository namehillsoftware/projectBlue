package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import java.io.IOException;
import java.util.List;

/**
 * Created by david on 12/17/16.
 */

public interface IPlaylistPlayerManager extends IPlaylistPlayer {

	IPlaylistPlayerManager startAsCompletable(List<IFile> playlist, int playlistStart, int fileStart) throws IOException;

	IPlaylistPlayerManager startAsCyclical(List<IFile> playlist, int playlistStart, int fileStart) throws IOException;

	IPlaylistPlayerManager continueAsCompletable();

	IPlaylistPlayerManager continueAsCyclical();
}
