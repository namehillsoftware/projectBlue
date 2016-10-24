package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.io.IOException;

/**
 * Created by david on 10/3/16.
 */
class MediaPlayerPreparerTask implements TwoParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise> {

	private final IFileUriProvider uriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	MediaPlayerPreparerTask(IFileUriProvider uriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.uriProvider = uriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject) {
		final MediaPlayer mediaPlayer;
		try {
			mediaPlayer = playbackInitialization.initializeMediaPlayer(uriProvider.getFileUri());
		} catch (IOException e) {
			reject.withError(e);
			return;
		}

		mediaPlayer.setOnPreparedListener(mp -> resolve.withResult(new MediaPlayerPlaybackHandler(mp)));

		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			reject.withError(new MediaPlayerException(mp, what, extra));
			return true;
		});

		mediaPlayer.prepareAsync();
	}
}
