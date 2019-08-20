package com.lasthopesoftware.bluewater.client.playback.playlist.playablefile;

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

import io.reactivex.ObservableEmitter;

public final class PlayableFilePlayer implements IPlaylistPlayer, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PlayableFilePlayer.class);

	private final Object stateChangeSync = new Object();
	private final PreparedPlayableFileQueue preparedPlaybackFileProvider;
	private final long preparedPosition;

	private PositionedPlayingFile positionedPlayingFile;
	private PositionedPlayableFile positionedPlayableFile;
	private Promise<?> lastStateChangePromise = Promise.empty();
	private float volume;

	private boolean isStarted;
	private ObservableEmitter<PositionedPlayingFile> emitter;

	private final CreateAndHold<PromisedResponse> lazyPausedPromise = new AbstractSynchronousLazy<PromisedResponse>() {
		@Override
		protected PromisedResponse create() {
			return o -> positionedPlayingFile != null
				? positionedPlayingFile
				.getPlayingFile()
				.promisePause()
				.then(p -> {
					positionedPlayableFile = new PositionedPlayableFile(
						p,
						positionedPlayingFile.getPlayableFileVolumeManager(),
						positionedPlayingFile.asPositionedFile());

					positionedPlayingFile = null;

					return null;
				})
				: Promise.empty();
		}
	};

	private final CreateAndHold<PromisedResponse> lazyResumePromise = new AbstractSynchronousLazy<PromisedResponse>() {
		@Override
		protected PromisedResponse create() {
			return o -> positionedPlayableFile != null
				? positionedPlayableFile
				.getPlayableFile()
				.promisePlayback()
				.then(p -> {
					positionedPlayingFile = new PositionedPlayingFile(
						p,
						positionedPlayingFile.getPlayableFileVolumeManager(),
						positionedPlayingFile.asPositionedFile());

					positionedPlayableFile = null;

					return null;
				})
				: Promise.empty();
		}
	};

	public PlayableFilePlayer(PreparedPlayableFileQueue preparedPlaybackFileProvider, long preparedPosition) {
		this.preparedPlaybackFileProvider = preparedPlaybackFileProvider;
		this.preparedPosition = preparedPosition;
	}

	@Override
	public synchronized void subscribe(ObservableEmitter<PositionedPlayingFile> e) {
		emitter = e;

		if (!isStarted) {
			isStarted = true;
			setupNextPreparedFile(preparedPosition);
		}
	}

	@SuppressWarnings("unchecked")
	public void pause() {
		synchronized (stateChangeSync) {
			lastStateChangePromise = lastStateChangePromise
				.eventually(
					lazyPausedPromise.getObject(),
					lazyPausedPromise.getObject());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void resume() {
		synchronized (stateChangeSync) {
			lastStateChangePromise = lastStateChangePromise
				.eventually(
					lazyResumePromise.getObject(),
					lazyResumePromise.getObject());
		}
	}

	@Override
	public boolean isPlaying() {
		return positionedPlayingFile != null;
	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;

		final PositionedPlayableFile positionedPlayableFile = this.positionedPlayableFile;
		if (positionedPlayableFile == null) return;

		positionedPlayableFile
			.getPlayableFileVolumeManager()
			.setVolume(volume);
	}

	private void setupNextPreparedFile() {
		setupNextPreparedFile(0);
	}

	private void setupNextPreparedFile(long preparedPosition) {
		final Promise<PositionedPlayableFile> preparingPlaybackFile =
			preparedPlaybackFileProvider
				.promiseNextPreparedPlaybackFile(preparedPosition);

		if (preparingPlaybackFile == null) {
			doCompletion();
			return;
		}

		preparingPlaybackFile
			.eventually(this::startFilePlayback)
			.excuse(new VoidResponse<>(this::handlePlaybackException));
	}

	private Promise<PlayingFile> startFilePlayback(PositionedPlayableFile positionedPlayableFile) {
		positionedPlayableFile.getPlayableFileVolumeManager().setVolume(volume);

		final PlayableFile playbackHandler = positionedPlayableFile.getPlayableFile();
		synchronized (stateChangeSync) {
			this.positionedPlayableFile = positionedPlayableFile;

			final Promise<PlayingFile> promisedPlayback = lastStateChangePromise.eventually(
				v -> playbackHandler.promisePlayback(),
				e -> playbackHandler.promisePlayback());

			lastStateChangePromise = promisedPlayback
				.then(playingFile -> {
					positionedPlayingFile = new PositionedPlayingFile(
						playingFile,
						positionedPlayableFile.getPlayableFileVolumeManager(),
						positionedPlayableFile.asPositionedFile());

					emitter.onNext(positionedPlayingFile);

					positionedPlayingFile
						.getPlayingFile()
						.promisePlayedFile()
						.then(p -> {
							closeAndStartNextFile(playbackHandler);
							return null;
						}, e -> {
							handlePlaybackException(e);
							return null;
						});

					return null;
				});

			return promisedPlayback;
		}
	}

	private void closeAndStartNextFile(PlayableFile playbackHandler) {
		try {
			playbackHandler.close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}

		setupNextPreparedFile();
	}

	private void haltPlayback() {
		synchronized (stateChangeSync) {
			lastStateChangePromise = lastStateChangePromise
				.eventually(
					generateHaltPromise(),
					generateHaltPromise())
				.then(p -> {
					if (p != null)
						p.close();
					doCompletion();
					return null;
				}, e -> {
					logger.error("There was an error releasing the media player", e);
					emitter.onError(e);

					return null;
				});
		}
	}

	private <T> PromisedResponse<T, PlayableFile> generateHaltPromise() {
		return o -> {
			if (positionedPlayableFile != null)
				return new Promise<>(positionedPlayableFile.getPlayableFile());

			if (positionedPlayingFile == null) return Promise.empty();

			return positionedPlayingFile
				.getPlayingFile()
				.promisePause();
		};
	}

	private void handlePlaybackException(Throwable exception) {
		emitter.onError(exception);

		haltPlayback();
	}

	@Override
	public void close() {
		haltPlayback();
	}

	private void doCompletion() {
		this.positionedPlayingFile = null;

		emitter.onComplete();
	}
}
