package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

/**
 * Created by david on 11/6/16.
 */

public interface IPlaybackPreparerTaskFactory {
	ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> getPlaybackPreparerTask(IFile file, int preparedAt);
}
