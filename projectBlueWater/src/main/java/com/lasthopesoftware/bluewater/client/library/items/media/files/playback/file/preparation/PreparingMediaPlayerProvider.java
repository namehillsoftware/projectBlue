package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by david on 9/26/16.
 */
public class PreparingMediaPlayerProvider implements
	IPreparedPlaybackFileProvider,
	OneParameterFunction<IBufferingPlaybackHandler, IPlaybackHandler>,
	OneParameterAction<IBufferingPlaybackHandler>
{
	private final IFileUriProvider fileUriProvider;
	private final Queue<IFile> playlist;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	private IPromise<IBufferingPlaybackHandler> nextPreparingMediaPlayerPromise;

	public PreparingMediaPlayerProvider(List<IFile> playlist, IFileUriProvider fileUriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.fileUriProvider = fileUriProvider;
		this.playlist = new LinkedList<>(playlist);
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public IPromise<IPlaybackHandler> promiseNextPreparedPlaybackFile() {
		return promiseNextPreparedPlaybackFile(0);
	}

	@Override
	public IPromise<IPlaybackHandler> promiseNextPreparedPlaybackFile(int preparedAt) {
		IPromise<IBufferingPlaybackHandler> bufferingPlaybackHandlerPromise = nextPreparingMediaPlayerPromise;

		if (bufferingPlaybackHandlerPromise == null)
			bufferingPlaybackHandlerPromise = getNextPreparingMediaPlayerPromise(preparedAt);

		nextPreparingMediaPlayerPromise = null;

		return
			bufferingPlaybackHandlerPromise
				.then((OneParameterFunction<IBufferingPlaybackHandler, IPlaybackHandler>) this);
	}

	private IPromise<IBufferingPlaybackHandler> getNextPreparingMediaPlayerPromise(int preparedAt) {
		return
			new Promise<>(
				new MediaPlayerPreparerTask(
					playlist.poll(),
					preparedAt,
					fileUriProvider,
					playbackInitialization));
	}

	@Override
	public IPlaybackHandler expectedUsing(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		bufferingPlaybackHandler.bufferPlaybackFile().then((OneParameterAction<IBufferingPlaybackHandler>) this);

		return bufferingPlaybackHandler;
	}

	@Override
	public void runWith(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		if (nextPreparingMediaPlayerPromise == null)
			nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise(0);
	}

	@Override
	public void close() throws IOException {

	}
}
