package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedPlaybackHandlerContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileProvider;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.callables.OneParameterVoidFunction;
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
	private final IPreparedPlaybackFileProvider preparedPlaybackFileProvider;
	private final int preparedPosition;
	private PositionedPlaybackHandlerContainer playbackHandlerContainer;
	private float volume;

	PlaylistPlaybackTask(IPreparedPlaybackFileProvider preparedPlaybackFileProvider, int preparedPosition) {
		this.preparedPlaybackFileProvider = preparedPlaybackFileProvider;
		this.preparedPosition = preparedPosition;
	}

	@Override
	public void runWith(IResolvedPromise<Void> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		setupNextPreparedFile(preparedPosition, resolve, reject, onCancelled);

		onCancelled.runWith(() -> {
			haltPlayback();

			reject.withError(new CancellationException("Playlist playback was cancelled"));
		});
	}

	public void pause() {
		if (playbackHandlerContainer == null) return;

		final IPlaybackHandler playbackHandler = playbackHandlerContainer.playbackHandler;

		if (playbackHandler.isPlaying()) playbackHandler.pause();
	}

	void resume() {
		if (playbackHandlerContainer != null && !playbackHandlerContainer.playbackHandler.isPlaying())
			playbackHandlerContainer.playbackHandler.promisePlayback();
	}

	public void setVolume(float volume) {
		this.volume = volume;
	}

	private void setupNextPreparedFile(IResolvedPromise<Void> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		setupNextPreparedFile(0, resolve, reject, onCancelled);
	}

	private void setupNextPreparedFile(int preparedPosition, IResolvedPromise<Void> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		final IPromise<PositionedPlaybackHandlerContainer> preparingPlaybackFile =
			preparedPlaybackFileProvider
				.promiseNextPreparedPlaybackFile(preparedPosition);

		if (preparingPlaybackFile == null) {
			resolve.withResult(null);
			return;
		}

		preparingPlaybackFile
			.then(new OneParameterVoidFunction<>(playbackHandlerContainer -> this.startFilePlayback(playbackHandlerContainer, resolve, reject, onCancelled)))
			.error(new OneParameterVoidFunction<>(exception -> handlePlaybackException(exception, reject)));
	}

	private void startFilePlayback(@NotNull PositionedPlaybackHandlerContainer playbackHandlerContainer, IResolvedPromise<Void> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {

		this.playbackHandlerContainer = playbackHandlerContainer;

		final IPlaybackHandler playbackHandler = playbackHandlerContainer.playbackHandler;

		playbackHandler.setVolume(volume);
		playbackHandler
			.promisePlayback()
			.then(new OneParameterVoidFunction<>(handler -> closeAndStartNextFile(handler, resolve, reject, onCancelled)))
			.error(new OneParameterVoidFunction<>(exception -> handlePlaybackException(exception, reject)));
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
		if (playbackHandlerContainer == null) return;

		try {
			playbackHandlerContainer.playbackHandler.close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}
	}

	private void handlePlaybackException(Exception exception, IRejectedPromise reject) {
		haltPlayback();

		reject.withError(exception);
	}
}
