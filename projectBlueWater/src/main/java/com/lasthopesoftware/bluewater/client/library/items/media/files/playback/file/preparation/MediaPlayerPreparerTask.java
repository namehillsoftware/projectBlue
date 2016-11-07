package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Created by david on 10/3/16.
 */
class MediaPlayerPreparerTask implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {

	private final IFile file;
	private final int prepareAt;
	private final IFileUriProvider uriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;
	private boolean isPrepared;

	MediaPlayerPreparerTask(IFile file, int prepareAt, IFileUriProvider uriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.file = file;
		this.prepareAt = prepareAt;
		this.uriProvider = uriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		final MediaPlayer mediaPlayer;
		try {
			mediaPlayer = playbackInitialization.initializeMediaPlayer(uriProvider.getFileUri(file));
			mediaPlayer.seekTo(prepareAt);
		} catch (IOException e) {
			reject.withError(e);
			return;
		}

		mediaPlayer.setOnPreparedListener(mp -> {
			isPrepared = true;
			resolve.withResult(new MediaPlayerPlaybackHandler(mp));
		});

		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			reject.withError(new MediaPlayerException(new MediaPlayerPlaybackHandler(mp), mp, what, extra));
			return true;
		});

		onCancelled.runWith(() -> {
			if (isPrepared) return;

			mediaPlayer.release();
			reject.withError(new InterruptedIOException("Media player preparation was interrupted"));
		});

		mediaPlayer.prepareAsync();
	}
}
