package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

/**
 * Created by david on 9/19/16.
 */
public interface IPlaybackFileErrorBroadcaster<TErrorData> {
	void setOnFileErrorListener(TwoParameterRunnable<IPlaybackFileErrorBroadcaster, TErrorData> listener);
	void broadcastFileError(TErrorData errorData);
}
