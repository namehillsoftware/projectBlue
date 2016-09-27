package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IMediaHandler;
import com.vedsoft.fluent.IFluentTask;

import java.io.IOException;

/**
 * Created by david on 9/24/16.
 */

public interface IPlaybackFilePreparer {
	IMediaHandler getMediaHandler() throws IOException;
}
