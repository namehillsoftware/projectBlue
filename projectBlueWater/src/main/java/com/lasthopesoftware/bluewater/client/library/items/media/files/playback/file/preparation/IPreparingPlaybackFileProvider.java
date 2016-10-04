package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import java.io.IOException;

/**
 * Created by david on 9/26/16.
 */

public interface IPreparingPlaybackFileProvider {
	IPlaybackFilePreparer getPreparingPlaybackFile(int pos) throws IOException;
}
