package com.lasthopesoftware.bluewater.client.playback.file.preparation.mediaplayer;

import android.media.MediaPlayer;
import android.net.Uri;

import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.MediaPlayerCloser;
import com.lasthopesoftware.bluewater.client.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.BufferingMediaPlayerFile;
import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerErrorException;
import com.lasthopesoftware.bluewater.client.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;
import com.lasthopesoftware.messenger.promises.response.PromisedResponse;

import java.io.IOException;
import java.util.concurrent.CancellationException;

final class MediaPlayerPreparerTask implements PromisedResponse<Uri, PreparedPlaybackFile> {

	private final long prepareAt;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	MediaPlayerPreparerTask(long prepareAt, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.prepareAt = prepareAt;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public Promise<PreparedPlaybackFile> promiseResponse(Uri uri) throws Throwable {
		return new Promise<>(new MediaPlayerPreparationOperator(uri, playbackInitialization, prepareAt));
	}

	private static final class MediaPlayerPreparationOperator implements MessengerOperator<PreparedPlaybackFile> {
		private final Uri uri;
		private final IPlaybackInitialization<MediaPlayer> playbackInitialization;
		private final long prepareAt;

		MediaPlayerPreparationOperator(Uri uri, IPlaybackInitialization<MediaPlayer> playbackInitialization, long prepareAt) {
			this.uri = uri;
			this.playbackInitialization = playbackInitialization;
			this.prepareAt = prepareAt;
		}

		@Override
		public void send(Messenger<PreparedPlaybackFile> messenger) {
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
				new MediaPlayerPreparationHandler(mediaPlayer, prepareAt, messenger, cancellationToken);

			mediaPlayer.setOnErrorListener(mediaPlayerPreparationHandler);

			mediaPlayer.setOnPreparedListener(mediaPlayerPreparationHandler);

			if (cancellationToken.isCancelled()) return;

			try {
				mediaPlayer.prepareAsync();
			} catch (IllegalStateException e) {
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
		private final Messenger<PreparedPlaybackFile> messenger;
		private final long prepareAt;
		private final CancellationToken cancellationToken;

		private MediaPlayerPreparationHandler(MediaPlayer mediaPlayer, long prepareAt, Messenger<PreparedPlaybackFile> messenger, CancellationToken cancellationToken) {
			this.mediaPlayer = mediaPlayer;
			this.prepareAt = prepareAt;
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
		public void onPrepared(MediaPlayer mp) {
			if (cancellationToken.isCancelled()) return;

			if (prepareAt > 0) {
				mediaPlayer.setOnSeekCompleteListener(this);
				mediaPlayer.seekTo((int)prepareAt);
				return;
			}

			messenger.sendResolution(new PreparedPlaybackFile(new MediaPlayerPlaybackHandler(mp), new BufferingMediaPlayerFile(mp)));
		}

		@Override
		public void onSeekComplete(MediaPlayer mp) {
			if (!cancellationToken.isCancelled())
				messenger.sendResolution(new PreparedPlaybackFile(new MediaPlayerPlaybackHandler(mp), new BufferingMediaPlayerFile(mp)));
		}

		@Override
		public void run() {
			cancellationToken.run();

			MediaPlayerCloser.closeMediaPlayer(mediaPlayer);

			messenger.sendRejection(new CancellationException());
		}
	}
}
