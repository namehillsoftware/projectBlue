package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

/**
 * Created by david on 9/26/16.
 */

public interface IPreparingPlaybackFileProvider {
	IPlaybackFilePreparer getPreparingPlaybackFile(int pos);
}
