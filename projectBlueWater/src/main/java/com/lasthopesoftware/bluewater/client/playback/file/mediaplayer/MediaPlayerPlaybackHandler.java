package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerErrorException;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerIllegalStateReporter;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress.MediaPlayerFileProgressReader;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;
import com.lasthopesoftware.bluewater.client.playback.file.progress.PollingProgressSource;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

public final class MediaPlayerPlaybackHandler
implements
	PlayableFile,
	MessengerOperator<PlayableFile>,
	MediaPlayer.OnCompletionListener,
	MediaPlayer.OnErrorListener,
	MediaPlayer.OnInfoListener,
	Runnable {

	private static final Logger logger = LoggerFactory.getLogger(MediaPlayerPlaybackHandler.class);
	private static final MediaPlayerIllegalStateReporter mediaPlayerIllegalStateReporter = new MediaPlayerIllegalStateReporter(MediaPlayerPlaybackHandler.class);

	private final MediaPlayer mediaPlayer;
	private final Promise<PlayableFile> playbackPromise;

	private Messenger<PlayableFile> playbackHandlerMessenger;

	private final CreateAndHold<PollingProgressSource> mediaPlayerPositionSource = new AbstractSynchronousLazy<PollingProgressSource>() {
		@Override
		protected PollingProgressSource create() {
			return new PollingProgressSource(
				new MediaPlayerFileProgressReader(mediaPlayer),
				Duration.millis(100));
		}
	};

	public MediaPlayerPlaybackHandler(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
		playbackPromise = new Promise<>((MessengerOperator<PlayableFile>) this);
	}

	@Override
	public boolean isPlaying() {
		try {
			return mediaPlayer.isPlaying();
		} catch (IllegalStateException e) {
			mediaPlayerIllegalStateReporter.reportIllegalStateException(e, "getting `isPlaying`");
			return false;
		}
	}

	@Override
	public void pause() {
		mediaPlayer.pause();
	}

	@Override
	public Observable<FileProgress> observeProgress(org.joda.time.Duration observationPeriod) {
		return Observable
			.create(mediaPlayerPositionSource.getObject().observePeriodically(observationPeriod))
			.sample(observationPeriod.getMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public synchronized Promise<PlayableFile> promisePlayback() {
		if (isPlaying()) return playbackPromise;

		try {
			mediaPlayer.start();
		} catch (IllegalStateException e) {
			close();

			playbackHandlerMessenger.sendRejection(new MediaPlayerException(this, mediaPlayer, e));
		}

		return playbackPromise;
	}

	@Override
	public void send(Messenger<PlayableFile> playbackHandlerMessenger) {
		this.playbackHandlerMessenger = playbackHandlerMessenger;

		playbackHandlerMessenger.cancellationRequested(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		playbackHandlerMessenger.sendResolution(this);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		playbackHandlerMessenger.sendRejection(new MediaPlayerErrorException(this, mp, what, extra));
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

		if (mediaPlayerPositionSource.isCreated())
			mediaPlayerPositionSource.getObject().cancel();

		MediaPlayerCloser.closeMediaPlayer(mediaPlayer);
	}

	@Override
	public void run() {
		close();
		playbackHandlerMessenger.sendRejection(new CancellationException());
	}
}