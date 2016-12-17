package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import android.support.annotation.Nullable;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CancellationException;

import rx.subjects.PublishSubject;

/**
 * Created by david on 11/8/16.
 */
final class PlaylistPlayerTask implements
	ThreeParameterAction<IResolvedPromise<Collection<PositionedPlaybackFile>>, IRejectedPromise, OneParameterAction<Runnable>> {

	private static final Logger logger = LoggerFactory.getLogger(PlaylistPlayerTask.class);
	private final IPreparedPlaybackFileQueue preparedPlaybackFileProvider;
	private final int preparedPosition;
	private PositionedPlaybackFile positionedPlaybackFile;
	private float volume;

	@NotNull private final Collection<PositionedPlaybackFile> completedPlaybackFiles = new LinkedList<>();
	@Nullable private final PublishSubject<PositionedPlaybackFile> playbackChangesPublisher;

	private IResolvedPromise<Collection<PositionedPlaybackFile>> resolve;
	private IRejectedPromise reject;

	PlaylistPlayerTask(@NotNull IPreparedPlaybackFileQueue preparedPlaybackFileProvider, int preparedPosition, @Nullable PublishSubject<PositionedPlaybackFile> playbackChangesPublisher) {
		this.preparedPlaybackFileProvider = preparedPlaybackFileProvider;
		this.preparedPosition = preparedPosition;
		this.playbackChangesPublisher = playbackChangesPublisher;
	}

	@Override
	public void runWith(IResolvedPromise<Collection<PositionedPlaybackFile>> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		this.resolve = resolve;
		this.reject = reject;

		setupNextPreparedFile(preparedPosition);

		onCancelled.runWith(() -> handlePlaybackException(new CancellationException("Playlist playback was cancelled")));
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

		final PositionedPlaybackFile positionedPlaybackFile = this.positionedPlaybackFile;
		if (positionedPlaybackFile != null)
			positionedPlaybackFile.getPlaybackHandler().setVolume(volume);
	}

	private void setupNextPreparedFile() {
		setupNextPreparedFile(0);
	}

	private void setupNextPreparedFile(int preparedPosition) {
		final IPromise<PositionedPlaybackFile> preparingPlaybackFile =
			preparedPlaybackFileProvider
				.promiseNextPreparedPlaybackFile(preparedPosition);

		if (preparingPlaybackFile == null) {
			resolve.withResult(completedPlaybackFiles);
			return;
		}

		preparingPlaybackFile
			.then(VoidFunc.running(this::startFilePlayback))
			.error(VoidFunc.running(this::handlePlaybackException));
	}

	private void startFilePlayback(@NotNull PositionedPlaybackFile positionedPlaybackFile) {

		this.positionedPlaybackFile = positionedPlaybackFile;

		if (playbackChangesPublisher != null)
			playbackChangesPublisher.onNext(positionedPlaybackFile);

		final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

		playbackHandler.setVolume(volume);
		playbackHandler
			.promisePlayback()
			.then(VoidFunc.running(this::closeAndStartNextFile))
			.error(VoidFunc.running(this::handlePlaybackException));
	}

	private void closeAndStartNextFile(IPlaybackHandler playbackHandler) {
		completedPlaybackFiles.add(positionedPlaybackFile);

		try {
			playbackHandler.close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}

		setupNextPreparedFile();
	}

	private void haltPlayback() {
		if (positionedPlaybackFile == null) return;

		try {
			positionedPlaybackFile.getPlaybackHandler().close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}
	}

	private void handlePlaybackException(Exception exception) {
		haltPlayback();

		reject.withError(exception);
	}
}
