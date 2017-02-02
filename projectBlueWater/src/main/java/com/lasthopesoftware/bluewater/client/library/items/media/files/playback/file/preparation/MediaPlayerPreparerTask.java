package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 10/3/16.
 */
class MediaPlayerPreparerTask implements CarelessOneParameterFunction<OneParameterAction<Runnable>, IBufferingPlaybackHandler> {

	private final static ExecutorService mediaPlayerPreparerExecutor = Executors.newSingleThreadExecutor();

	private final IFile file;
	private final int prepareAt;
	private final IFileUriProvider uriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;
	private boolean isCancelled;

	MediaPlayerPreparerTask(IFile file, int prepareAt, IFileUriProvider uriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.file = file;
		this.prepareAt = prepareAt;
		this.uriProvider = uriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public IBufferingPlaybackHandler resultFrom(OneParameterAction<Runnable> onCancelled) throws Exception {
		final MediaPlayer mediaPlayer = playbackInitialization.initializeMediaPlayer(uriProvider.getFileUri(file));

		onCancelled.runWith(() -> {
			isCancelled = true;

			mediaPlayer.release();
		});

		if (isCancelled) return null;

		mediaPlayer.prepare();

		if (isCancelled) return null;

		mediaPlayer.seekTo(prepareAt);

		if (isCancelled) return null;
		return new MediaPlayerPlaybackHandler(mediaPlayer);
	}
}
