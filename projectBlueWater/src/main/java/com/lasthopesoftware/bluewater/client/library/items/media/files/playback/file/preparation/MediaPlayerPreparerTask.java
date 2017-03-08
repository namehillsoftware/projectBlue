package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import java.io.IOException;
import java.util.concurrent.CancellationException;

/**
 * Created by david on 10/3/16.
 */
final class MediaPlayerPreparerTask implements
	ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {

	private final IFile file;
	private final int prepareAt;
	private final IFileUriProvider uriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

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
		} catch (IOException e) {
			reject.withError(e);
			return;
		}

		final MediaPlayerPreparationHandler mediaPlayerPreparationHandler =
			new MediaPlayerPreparationHandler(mediaPlayer, prepareAt, resolve, reject);

		onCancelled.runWith(mediaPlayerPreparationHandler);

		if (mediaPlayerPreparationHandler.isCancelled()) {
			reject.withError(new CancellationException());
			return;
		}

		mediaPlayer.setOnErrorListener(mediaPlayerPreparationHandler);

		mediaPlayer.setOnPreparedListener(mediaPlayerPreparationHandler);

		try {
			mediaPlayer.prepare();
		} catch (IOException e) {
			reject.withError(e);
		}
	}

	private static final class MediaPlayerPreparationHandler
		implements
			MediaPlayer.OnErrorListener,
			MediaPlayer.OnPreparedListener,
			MediaPlayer.OnSeekCompleteListener,
			Runnable
	{
		private final MediaPlayer mediaPlayer;
		private final IResolvedPromise<IBufferingPlaybackHandler> resolve;
		private final IRejectedPromise reject;
		private final int prepareAt;

		private boolean isCancelled;

		private MediaPlayerPreparationHandler(MediaPlayer mediaPlayer, int prepareAt, IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject) {
			this.mediaPlayer = mediaPlayer;
			this.resolve = resolve;
			this.reject = reject;
			this.prepareAt = prepareAt;
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			reject.withError(new MediaPlayerException(new EmptyPlaybackHandler(0), mp, what, extra));
			return true;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			if (isCancelled) {
				reject.withError(new CancellationException());
				return;
			}

			if (prepareAt > 0) {
				mediaPlayer.setOnSeekCompleteListener(this);
				mediaPlayer.seekTo(prepareAt);
				return;
			}

			resolve.withResult(new MediaPlayerPlaybackHandler(mp));
		}

		@Override
		public void onSeekComplete(MediaPlayer mp) {
			if (isCancelled) {
				reject.withError(new CancellationException());
				return;
			}

			resolve.withResult(new MediaPlayerPlaybackHandler(mp));
		}

		@Override
		public void run() {
			isCancelled = true;

			mediaPlayer.release();
		}

		public boolean isCancelled() {
			return isCancelled;
		}
	}
}
