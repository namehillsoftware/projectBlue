package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

/**
 * Created by david on 11/6/16.
 */
public class MediaPlayerPlaybackPreparerTaskFactory implements IPlaybackPreparerTaskFactory {
	private final IFileUriProvider fileUriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	public MediaPlayerPlaybackPreparerTaskFactory(IFileUriProvider fileUriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.fileUriProvider = fileUriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> getPlaybackPreparerTask(IFile file, int preparedAt) {
		return new MediaPlayerPreparerTask(file, preparedAt, fileUriProvider, playbackInitialization);
	}
}
