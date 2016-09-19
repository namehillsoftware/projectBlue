package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileErrorListener;

/**
 * Created by david on 9/19/16.
 */
public interface IPlaybackFileErrorBroadcaster {
	void setOnFileErrorListener(OnFileErrorListener listener);
}
