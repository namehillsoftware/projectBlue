package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import java.util.List;

/**
 * Created by david on 11/1/16.
 */

public interface IProvidePlaybackQueues {
	IPreparedPlaybackFileProvider getQueue(List<IFile> playlist, int startingAt, boolean isCyclical);
}
