package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error;

import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 9/19/16.
 */
public interface IPlaybackFileErrorBroadcaster<TErrorData> {
	void setOnFileErrorListener(TwoParameterAction<IPlaybackFileErrorBroadcaster, TErrorData> listener);
	void broadcastFileError(TErrorData errorData);
}
