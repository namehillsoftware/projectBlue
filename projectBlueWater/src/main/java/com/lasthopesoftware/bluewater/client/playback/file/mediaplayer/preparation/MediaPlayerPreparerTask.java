package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.preparation;

import android.media.MediaPlayer;
import android.net.Uri;

import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerCloser;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.buffering.BufferingMediaPlayerFile;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerErrorException;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class MediaPlayerPreparerTask implements PromisedResponse<Uri, PreparedPlayableFile> {

	private final static ExecutorService mediaPlayerPreparerExecutor = Executors.newCachedThreadPool();

	private final long prepareAt;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	MediaPlayerPreparerTask(long prepareAt, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.prepareAt = prepareAt;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public Promise<PreparedPlayableFile> promiseResponse(Uri uri) throws Throwable {
		return new QueuedPromise<>(new MediaPlayerPreparationOperator(uri, playbackInitialization, prepareAt), mediaPlayerPreparerExecutor);
	}

	private static final class MediaPlayerPreparationOperator implements MessengerOperator<PreparedPlayableFile> {
		private final Uri uri;
		private final IPlaybackInitialization<MediaPlayer> playbackInitialization;
		private final long prepareAt;

		MediaPlayerPreparationOperator(Uri uri, IPlaybackInitialization<MediaPlayer> playbackInitialization, long prepareAt) {
			this.uri = uri;
			this.playbackInitialization = playbackInitialization;
			this.prepareAt = prepareAt;
		}

		@Override
		public void send(Messenger<PreparedPlayableFile> messenger) {
			final CancellationToken cancellationToken = new CancellationToken();
			messenger.cancellationRequested(cancellationToken);

			if (cancellationToken.isCancelled()) {
				messenger.sendRejection(new CancellationException());
				return;
			}

			final MediaPlayer mediaPlayer;
			try {
				mediaPlayer = playbackInitialization.initializeMediaPlayer(uri);
			} catch (IOException e) {
				messenger.sendRejection(e);
				return;
			}

			if (cancellationToken.isCancelled()) {
				MediaPlayerCloser.closeMediaPlayer(mediaPlayer);
				messenger.sendRejection(new CancellationException());
				return;
			}

			final MediaPlayerPreparationHandler mediaPlayerPreparationHandler =
				new MediaPlayerPreparationHandler(mediaPlayer, messenger, cancellationToken);

			mediaPlayer.setOnErrorListener(mediaPlayerPreparationHandler);

			if (cancellationToken.isCancelled()) return;

			try {
				mediaPlayer.prepare();
			} catch (IllegalStateException | IOException e) {
				messenger.sendRejection(e);
			}

			if (cancellationToken.isCancelled()) {
				MediaPlayerCloser.closeMediaPlayer(mediaPlayer);
				messenger.sendRejection(new CancellationException());
				return;
			}

			if (prepareAt <= 0) {
				messenger.sendResolution(new PreparedPlayableFile(new MediaPlayerPlaybackHandler(mediaPlayer), new BufferingMediaPlayerFile(mediaPlayer)));
				return;
			}

			mediaPlayer.setOnSeekCompleteListener(mediaPlayerPreparationHandler);
			mediaPlayer.seekTo((int)prepareAt);
		}
	}

	private static final class MediaPlayerPreparationHandler
		implements
		MediaPlayer.OnErrorListener,
		MediaPlayer.OnSeekCompleteListener,
		Runnable
	{
		private final MediaPlayer mediaPlayer;
		private final Messenger<PreparedPlayableFile> messenger;
		private final CancellationToken cancellationToken;

		private MediaPlayerPreparationHandler(MediaPlayer mediaPlayer, Messenger<PreparedPlayableFile> messenger, CancellationToken cancellationToken) {
			this.mediaPlayer = mediaPlayer;
			this.messenger = messenger;
			this.cancellationToken = cancellationToken;
			messenger.cancellationRequested(this);
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			messenger.sendRejection(new MediaPlayerErrorException(new EmptyPlaybackHandler(0), mp, what, extra));
			return true;
		}

		@Override
		public void onSeekComplete(MediaPlayer mp) {
			if (!cancellationToken.isCancelled())
				messenger.sendResolution(new PreparedPlayableFile(new MediaPlayerPlaybackHandler(mp), new BufferingMediaPlayerFile(mp)));
		}

		@Override
		public void run() {
			cancellationToken.run();

			MediaPlayerCloser.closeMediaPlayer(mediaPlayer);

			messenger.sendRejection(new CancellationException());
		}
	}
}