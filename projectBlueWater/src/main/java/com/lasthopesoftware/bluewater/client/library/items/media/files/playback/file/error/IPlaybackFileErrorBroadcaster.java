package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;

/**
 * Created by david on 9/19/16.
 */
public interface IPlaybackFileErrorBroadcaster {
	void setOnFileErrorListener(OnFileErrorListener listener);
	void broadcastFileError(IPlaybackFile mediaPlayer, int what, int extra);
}
