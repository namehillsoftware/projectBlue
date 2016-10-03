package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.vedsoft.fluent.FluentCallable;

import java.io.IOException;

/**
 * Created by david on 9/24/16.
 */

public interface IPlaybackFilePreparer {
	FluentCallable<IPlaybackHandler> getMediaHandler();
}
