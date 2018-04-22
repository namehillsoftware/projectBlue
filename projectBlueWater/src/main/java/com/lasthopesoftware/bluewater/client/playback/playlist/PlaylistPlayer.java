package com.lasthopesoftware.bluewater.client.playback.playlist;

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.volume.IVolumeManagement;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

import io.reactivex.ObservableEmitter;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;

public final class PlaylistPlayer implements IPlaylistPlayer, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PlaylistPlayer.class);
	private final PreparedPlayableFileQueue preparedPlaybackFileProvider;
	private final IPlaybackHandlerVolumeControllerFactory volumeControllerFactory;
	private final long preparedPosition;
	private PositionedPlayingFile positionedPlayingFile;
	private float volume;

	private volatile boolean isStarted;
	private ObservableEmitter<PositionedPlayingFile> emitter;
	private IVolumeManagement volumeManager;

	public PlaylistPlayer(PreparedPlayableFileQueue preparedPlaybackFileProvider, IPlaybackHandlerVolumeControllerFactory volumeControllerFactory, long preparedPosition) {
		this.preparedPlaybackFileProvider = preparedPlaybackFileProvider;
		this.volumeControllerFactory = volumeControllerFactory;
		this.preparedPosition = preparedPosition;
	}

	@Override
	public void subscribe(ObservableEmitter<PositionedPlayingFile> e) {
		emitter = e;

		if (!isStarted) {
			isStarted = true;
			setupNextPreparedFile(preparedPosition);
		}
	}

	public void pause() {
		if (positionedPlayingFile == null) return;

		final PlayableFile playbackHandler = positionedPlayingFile.getPlayingFile();

		if (playbackHandler.isPlaying()) playbackHandler.pause();
	}

	@Override
	public void resume() {
		if (positionedPlayingFile != null)
			positionedPlayingFile.getPlayingFile().promisePlayback();
	}

	@Override
	public boolean isPlaying() {
		return positionedPlayingFile != null;
	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;

		final IVolumeManagement volumeManager = this.volumeManager;
		if (volumeManager != null)
			volumeManager.setVolume(volume);
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
			.then(this::changeVolumeManager)
			.eventually(this::startFilePlayback)
			.excuse(perform(this::handlePlaybackException));
	}

	private PositionedPlayableFile changeVolumeManager(PositionedPlayableFile positionedPlayingFile) {
		this.volumeManager = volumeControllerFactory.manageVolume(positionedPlayingFile, volume);
		return positionedPlayingFile;
	}

	private Promise<PlayingFile> startFilePlayback(PositionedPlayableFile positionedPlayableFile) {
		final PlayableFile playbackHandler = positionedPlayableFile.getPlayableFile();

		final Promise<PlayingFile> promisedPlayback = playbackHandler.promisePlayback();

		promisedPlayback
			.then(playingFile -> {
				this.positionedPlayingFile = new PositionedPlayingFile(
					playingFile,
					positionedPlayableFile.getPlayableFileVolumeManager(),
					positionedPlayableFile.asPositionedFile());

				emitter.onNext(this.positionedPlayingFile);

				playingFile.observeProgress(Duration.millis(Long.MAX_VALUE))
					.doOnComplete(() -> closeAndStartNextFile(playbackHandler));

				return null;
			});

		return promisedPlayback;
	}

	private void closeAndStartNextFile(PlayableFile playbackHandler) {
		try {
			playbackHandler.close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}

		volumeManager = null;

		setupNextPreparedFile();
	}

	private void haltPlayback() {
		if (positionedPlayingFile == null) return;

		positionedPlayingFile.getPlayingFile()
			.promisePause()
			.then(p -> {
				p.close();
				doCompletion();
				return null;
			})
			.excuse(e -> {
				logger.error("There was an error releasing the media player", e);
				emitter.onError(e);

				return null;
			});
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
