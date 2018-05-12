package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerErrorException;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerIllegalStateReporter;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress.MediaPlayerFileProgressReader;
import com.lasthopesoftware.bluewater.client.playback.file.progress.NotifyFilePlaybackComplete;
import com.lasthopesoftware.bluewater.client.playback.file.progress.NotifyFilePlaybackError;
import com.lasthopesoftware.bluewater.client.playback.file.progress.PromisedPlayedFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.vedsoft.futures.runnables.OneParameterAction;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaPlayerPlaybackHandler
implements
	PlayableFile,
	PlayingFile,
	MediaPlayer.OnCompletionListener,
	MediaPlayer.OnErrorListener,
	MediaPlayer.OnInfoListener,
	NotifyFilePlaybackComplete,
	NotifyFilePlaybackError<MediaPlayerErrorException>,
	Runnable {

	private static final Logger logger = LoggerFactory.getLogger(MediaPlayerPlaybackHandler.class);
	private static final MediaPlayerIllegalStateReporter mediaPlayerIllegalStateReporter = new MediaPlayerIllegalStateReporter(MediaPlayerPlaybackHandler.class);

	private final MediaPlayer mediaPlayer;

	private Runnable playbackCompletedAction;
	private OneParameterAction<MediaPlayerErrorException> playbackErrorAction;

	private final CreateAndHold<MediaPlayerFileProgressReader> lazyFileProgressReader = new AbstractSynchronousLazy<MediaPlayerFileProgressReader>() {
		@Override
		protected MediaPlayerFileProgressReader create() {
			return new MediaPlayerFileProgressReader(mediaPlayer);
		}
	};

	private final CreateAndHold<ProgressingPromise<Duration, PlayedFile>> mediaPlayerPositionSource = new AbstractSynchronousLazy<ProgressingPromise<Duration, PlayedFile>>() {
		@Override
		protected ProgressingPromise<Duration, PlayedFile> create() {
			return new PromisedPlayedFile<>(
				lazyFileProgressReader.getObject(),
				MediaPlayerPlaybackHandler.this,
				MediaPlayerPlaybackHandler.this);
		}
	};

	public MediaPlayerPlaybackHandler(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnInfoListener(this);
	}

	private boolean isPlaying() {
		try {
			return mediaPlayer.isPlaying();
		} catch (IllegalStateException e) {
			mediaPlayerIllegalStateReporter.reportIllegalStateException(e, "getting `isPlaying`");
			return false;
		}
	}

	private void pause() {
		mediaPlayer.pause();
	}

	@Override
	public Duration getProgress() {
		return null;
	}

	@Override
	public Promise<PlayableFile> promisePause() {
		pause();
		return new Promise<>(this);
	}

	@Override
	public ProgressingPromise<Duration, PlayedFile> promisePlayedFile() {
		return mediaPlayerPositionSource.getObject();
	}

	@Override
	public synchronized Duration getDuration() {
		return lazyFileProgressReader.getObject().getProgress();
	}

	@Override
	public synchronized Promise<PlayingFile> promisePlayback() {
		if (isPlaying()) return new Promise<>(this);

		try {
			mediaPlayer.start();
		} catch (IllegalStateException e) {
			close();

			return new Promise<>(new MediaPlayerException(this, mediaPlayer, e));
		}

		return new Promise<>(this);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (playbackCompletedAction != null)
			playbackCompletedAction.run();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		final MediaPlayerErrorException e = new MediaPlayerErrorException(this, mp, what, extra);
		if (playbackErrorAction != null)
			playbackErrorAction.runWith(e);
		return true;
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		logger.warn("The media player reported the following - " + what + " - " + extra);
		return true;
	}

	@Override
	public void close() {
		logger.info("Closing the media player");

		try {
			if (isPlaying())
				mediaPlayer.stop();
		} catch (IllegalStateException se) {
			mediaPlayerIllegalStateReporter.reportIllegalStateException(se, "stopping");
		}

		MediaPlayerCloser.closeMediaPlayer(mediaPlayer);
	}

	@Override
	public void run() {
		close();
	}

	@Override
	public void playbackCompleted(Runnable runnable) {
		playbackCompletedAction = runnable;
	}

	@Override
	public void playbackError(OneParameterAction<MediaPlayerErrorException> onError) {
		playbackErrorAction = onError;
	}
}