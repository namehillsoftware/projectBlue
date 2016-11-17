package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.callables.VoidFunction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CancellationException;

/**
 * Created by david on 11/8/16.
 */
final class PlaylistPlaybackTask implements ThreeParameterAction<IResolvedPromise<Void>, IRejectedPromise, OneParameterAction<Runnable>> {

	private static final Logger logger = LoggerFactory.getLogger(PlaylistPlaybackTask.class);
	private final IPreparedPlaybackFileQueue preparedPlaybackFileProvider;
	private final int preparedPosition;
	private PositionedPlaybackFile positionedPlaybackFile;
	private float volume;

	PlaylistPlaybackTask(IPreparedPlaybackFileQueue preparedPlaybackFileProvider, int preparedPosition) {
		this.preparedPlaybackFileProvider = preparedPlaybackFileProvider;
		this.preparedPosition = preparedPosition;
	}

	@Override
	public void runWith(IResolvedPromise<Void> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		setupNextPreparedFile(preparedPosition, resolve, reject, onCancelled);

		onCancelled.runWith(() -> handlePlaybackException(new CancellationException("Playlist playback was cancelled"), reject));
	}

	public void pause() {
		if (positionedPlaybackFile == null) return;

		final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

		if (playbackHandler.isPlaying()) playbackHandler.pause();
	}

	void resume() {
		if (positionedPlaybackFile != null && !positionedPlaybackFile.getPlaybackHandler().isPlaying())
			positionedPlaybackFile.getPlaybackHandler().promisePlayback();
	}

	void setVolume(float volume) {
		this.volume = volume;
	}

	private void setupNextPreparedFile(IResolvedPromise<Void> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		setupNextPreparedFile(0, resolve, reject, onCancelled);
	}

	private void setupNextPreparedFile(int preparedPosition, IResolvedPromise<Void> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		final IPromise<PositionedPlaybackFile> preparingPlaybackFile =
			preparedPlaybackFileProvider
				.promiseNextPreparedPlaybackFile(preparedPosition);

		if (preparingPlaybackFile == null) {
			resolve.withResult(null);
			return;
		}

		preparingPlaybackFile
			.then(new VoidFunction<>(playbackHandlerContainer -> this.startFilePlayback(playbackHandlerContainer, resolve, reject, onCancelled)))
			.error(new VoidFunction<>(exception -> handlePlaybackException(exception, reject)));
	}

	private void startFilePlayback(@NotNull PositionedPlaybackFile positionedPlaybackFile, IResolvedPromise<Void> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {

		this.positionedPlaybackFile = positionedPlaybackFile;

		final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

		playbackHandler.setVolume(volume);
		playbackHandler
			.promisePlayback()
			.then(new VoidFunction<>(handler -> closeAndStartNextFile(handler, resolve, reject, onCancelled)))
			.error(new VoidFunction<>(exception -> handlePlaybackException(exception, reject)));
	}

	private void closeAndStartNextFile(IPlaybackHandler playbackHandler, IResolvedPromise<Void> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		try {
			playbackHandler.close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}

		setupNextPreparedFile(resolve, reject, onCancelled);
	}

	private void haltPlayback() {
		if (positionedPlaybackFile == null) return;

		try {
			positionedPlaybackFile.getPlaybackHandler().close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}
	}

	private void handlePlaybackException(Exception exception, IRejectedPromise reject) {
		haltPlayback();

		reject.withError(exception);
	}
}
