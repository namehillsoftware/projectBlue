package com.lasthopesoftware.bluewater.client.playback.playlist;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;
import com.lasthopesoftware.messenger.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

import io.reactivex.ObservableEmitter;

import static com.lasthopesoftware.messenger.promises.response.ImmediateAction.perform;

public final class PlaylistPlayer implements IPlaylistPlayer, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PlaylistPlayer.class);
	private final IPreparedPlaybackFileQueue preparedPlaybackFileProvider;
	private final IPlaybackHandlerVolumeControllerFactory volumeControllerFactory;
	private final int preparedPosition;
	private PositionedPlaybackFile positionedPlaybackFile;
	private float volume;

	private volatile boolean isStarted;
	private ObservableEmitter<PositionedPlaybackFile> emitter;
	private IVolumeManagement volumeManager;

	public PlaylistPlayer(IPreparedPlaybackFileQueue preparedPlaybackFileProvider, IPlaybackHandlerVolumeControllerFactory volumeControllerFactory, int preparedPosition) {
		this.preparedPlaybackFileProvider = preparedPlaybackFileProvider;
		this.volumeControllerFactory = volumeControllerFactory;
		this.preparedPosition = preparedPosition;
	}

	@Override
	public void subscribe(ObservableEmitter<PositionedPlaybackFile> e) throws Exception {
		emitter = e;

		if (!isStarted) {
			isStarted = true;
			setupNextPreparedFile(preparedPosition);
		}
	}

	public void pause() {
		if (positionedPlaybackFile == null) return;

		final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

		if (playbackHandler.isPlaying()) playbackHandler.pause();
	}

	@Override
	public void resume() {
		if (positionedPlaybackFile != null)
			positionedPlaybackFile.getPlaybackHandler().promisePlayback();
	}

	@Override
	public boolean isPlaying() {
		return positionedPlaybackFile != null && positionedPlaybackFile.getPlaybackHandler().isPlaying();
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

	private void setupNextPreparedFile(int preparedPosition) {
		final Promise<PositionedPlaybackFile> preparingPlaybackFile =
			preparedPlaybackFileProvider
				.promiseNextPreparedPlaybackFile(preparedPosition);

		if (preparingPlaybackFile == null) {
			doCompletion();
			return;
		}

		preparingPlaybackFile
			.then(this::changeVolumeManager)
			.then(this::changePlaybackFile)
			.eventually(this::startFilePlayback)
			.excuse(perform(this::handlePlaybackException));
	}

	private PositionedPlaybackFile changeVolumeManager(PositionedPlaybackFile positionedPlaybackFile) {
		this.volumeManager = volumeControllerFactory.manageVolume(positionedPlaybackFile, volume);
		return positionedPlaybackFile;
	}

	private PositionedPlaybackFile changePlaybackFile(PositionedPlaybackFile positionedPlaybackFile) {
		this.positionedPlaybackFile = positionedPlaybackFile;

		emitter.onNext(this.positionedPlaybackFile);

		return positionedPlaybackFile;
	}

	private Promise<IPlaybackHandler> startFilePlayback(PositionedPlaybackFile positionedPlaybackFile) {
		final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

		final Promise<IPlaybackHandler> promisedPlayback = playbackHandler.promisePlayback();

		promisedPlayback.then(perform(this::closeAndStartNextFile));

		return promisedPlayback;
	}

	private void closeAndStartNextFile(IPlaybackHandler playbackHandler) {
		try {
			playbackHandler.close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}

		volumeManager = null;

		setupNextPreparedFile();
	}

	private void haltPlayback() {
		try {
			if (positionedPlaybackFile != null)
				positionedPlaybackFile.getPlaybackHandler().close();

			doCompletion();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
			emitter.onError(e);
		}
	}

	private void handlePlaybackException(Throwable exception) {
		emitter.onError(exception);

		haltPlayback();
	}

	@Override
	public void close() throws IOException {
		haltPlayback();
	}

	private void doCompletion() {
		this.positionedPlaybackFile = null;

		emitter.onComplete();
	}
}
