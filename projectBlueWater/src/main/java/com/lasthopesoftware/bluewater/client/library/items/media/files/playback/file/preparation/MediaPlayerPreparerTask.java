package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;
import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
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

import java.util.concurrent.CancellationException;

/**
 * Created by david on 10/3/16.
 */
final class MediaPlayerPreparerTask implements
	ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {

	private final File file;
	private final int prepareAt;
	private final IFileUriProvider uriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	MediaPlayerPreparerTask(File file, int prepareAt, IFileUriProvider uriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.file = file;
		this.prepareAt = prepareAt;
		this.uriProvider = uriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		uriProvider
			.getFileUri(file)
			.then(new MediaPlayerPreparationTask(playbackInitialization, prepareAt, resolve, reject, onCancelled))
			.error(new UriProviderErrorHandler(reject));
	}

	private static final class MediaPlayerPreparationTask implements CarelessOneParameterFunction<Uri, Void> {
		private final IPlaybackInitialization<MediaPlayer> playbackInitialization;
		private final int prepareAt;
		private final IResolvedPromise<IBufferingPlaybackHandler> resolve;
		private final IRejectedPromise reject;
		private final OneParameterAction<Runnable> onCancelled;

		MediaPlayerPreparationTask(IPlaybackInitialization<MediaPlayer> playbackInitialization, int prepareAt, IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			this.playbackInitialization = playbackInitialization;
			this.prepareAt = prepareAt;
			this.resolve = resolve;
			this.reject = reject;
			this.onCancelled = onCancelled;
		}

		@Override
		public Void resultFrom(Uri uri) throws Exception {
			final MediaPlayer mediaPlayer;
			mediaPlayer = playbackInitialization.initializeMediaPlayer(uri);

			final MediaPlayerPreparationHandler mediaPlayerPreparationHandler =
				new MediaPlayerPreparationHandler(mediaPlayer, prepareAt, resolve, reject);

			onCancelled.runWith(mediaPlayerPreparationHandler);

			if (mediaPlayerPreparationHandler.isCancelled()) {
				reject.withError(new CancellationException());
				return null;
			}

			mediaPlayer.setOnErrorListener(mediaPlayerPreparationHandler);

			mediaPlayer.setOnPreparedListener(mediaPlayerPreparationHandler);

			mediaPlayer.prepare();

			return null;
		}
	}

	private static final class UriProviderErrorHandler implements CarelessOneParameterFunction<Throwable, Void> {

		private final IRejectedPromise reject;

		UriProviderErrorHandler(IRejectedPromise reject) {
			this.reject = reject;
		}

		@Override
		public Void resultFrom(Throwable throwable) throws Exception {
			reject.withError(throwable);
			return null;
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
