package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.shared.DispatchedPromise.DispatchedPromise;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 11/6/16.
 */
public class MediaPlayerPlaybackPreparerPromiseFactory implements IPlaybackPreparerPromiseFactory {
	private final IFileUriProvider fileUriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	public MediaPlayerPlaybackPreparerPromiseFactory(IFileUriProvider fileUriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.fileUriProvider = fileUriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public IPromise<IBufferingPlaybackHandler> getPlaybackPreparerPromise(IFile file, int preparedAt) {
		return new DispatchedPromise<>(new MediaPlayerPreparerTask(file, preparedAt, fileUriProvider, playbackInitialization));
	}
}
