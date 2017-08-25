package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import android.media.MediaPlayer;
import android.net.Uri;

import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class MediaPlayerPreparerTask implements CarelessOneParameterFunction<Uri, Promise<IBufferingPlaybackHandler>> {

	private final static ExecutorService mediaPlayerPreparerExecutor = Executors.newSingleThreadExecutor();

	private final int prepareAt;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	MediaPlayerPreparerTask(int prepareAt, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.prepareAt = prepareAt;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public Promise<IBufferingPlaybackHandler> resultFrom(Uri uri) throws Throwable {
		return new QueuedPromise<>(new MediaPlayerPreparationTask(uri, playbackInitialization, prepareAt), mediaPlayerPreparerExecutor);
	}

	private static final class MediaPlayerPreparationTask implements OneParameterAction<Messenger<IBufferingPlaybackHandler>> {
		private final Uri uri;
		private final IPlaybackInitialization<MediaPlayer> playbackInitialization;
		private final int prepareAt;

		MediaPlayerPreparationTask(Uri uri, IPlaybackInitialization<MediaPlayer> playbackInitialization, int prepareAt) {
			this.uri = uri;
			this.playbackInitialization = playbackInitialization;
			this.prepareAt = prepareAt;
		}

		@Override
		public void runWith(Messenger<IBufferingPlaybackHandler> messenger) {
			final MediaPlayer mediaPlayer;
			try {
				mediaPlayer = playbackInitialization.initializeMediaPlayer(uri);
			} catch (IOException e) {
				messenger.sendRejection(e);
				return;
			}

			final MediaPlayerPreparationHandler mediaPlayerPreparationHandler =
				new MediaPlayerPreparationHandler(mediaPlayer, prepareAt, messenger);

			messenger.cancellationRequested(mediaPlayerPreparationHandler);

			mediaPlayer.setOnErrorListener(mediaPlayerPreparationHandler);

			mediaPlayer.setOnPreparedListener(mediaPlayerPreparationHandler);

			try {
				mediaPlayer.prepare();
			} catch (IllegalStateException | IOException e) {
				messenger.sendRejection(e);
			}
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
		private final Messenger<IBufferingPlaybackHandler> messenger;
		private final int prepareAt;

		private boolean isCancelled;

		private MediaPlayerPreparationHandler(MediaPlayer mediaPlayer, int prepareAt, Messenger<IBufferingPlaybackHandler> messenger) {
			this.mediaPlayer = mediaPlayer;
			this.prepareAt = prepareAt;
			this.messenger = messenger;
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			messenger.sendRejection(new MediaPlayerException(new EmptyPlaybackHandler(0), mp, what, extra));
			return true;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			if (isCancelled) {
				messenger.sendRejection(new CancellationException());
				return;
			}

			if (prepareAt > 0) {
				mediaPlayer.setOnSeekCompleteListener(this);
				mediaPlayer.seekTo(prepareAt);
				return;
			}

			messenger.sendResolution(new MediaPlayerPlaybackHandler(mp));
		}

		@Override
		public void onSeekComplete(MediaPlayer mp) {
			if (isCancelled) {
				messenger.sendRejection(new CancellationException());
				return;
			}

			messenger.sendResolution(new MediaPlayerPlaybackHandler(mp));
		}

		@Override
		public void run() {
			isCancelled = true;
			mediaPlayer.release();

			messenger.sendRejection(new CancellationException());
		}
	}
}
