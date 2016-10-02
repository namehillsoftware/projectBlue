package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;

import java.io.IOException;

/**
 * Created by david on 9/24/16.
 */

public interface IPlaybackFilePreparer {
	IPlaybackHandler getMediaHandler() throws IOException;
}
