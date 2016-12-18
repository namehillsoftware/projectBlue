package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import android.support.annotation.Nullable;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.futures.callables.VoidFunc;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CancellationException;

import io.reactivex.Observable;
import io.reactivex.Observer;

/**
 * Created by david on 11/8/16.
 */
public final class PlaylistPlayer extends Observable<PositionedPlaybackFile> implements IPlaylistPlayer {

	private static final Logger logger = LoggerFactory.getLogger(PlaylistPlayer.class);
	private final IPreparedPlaybackFileQueue preparedPlaybackFileProvider;
	private PositionedPlaybackFile positionedPlaybackFile;
	private float volume;

	private final Collection<PositionedPlaybackFile> previousPlaybackFileChanges = new ArrayList<>();
	@Nullable private Observer<? super PositionedPlaybackFile> observer;

	private boolean isCompleted;

	public PlaylistPlayer(@NotNull IPreparedPlaybackFileQueue preparedPlaybackFileProvider, int preparedPosition) {
		this.preparedPlaybackFileProvider = preparedPlaybackFileProvider;
		setupNextPreparedFile(preparedPosition);
	}

	public void pause() {
		if (positionedPlaybackFile == null) return;

		final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

		if (playbackHandler.isPlaying()) playbackHandler.pause();
	}

	@Override
	public void resume() {
		if (positionedPlaybackFile != null && !positionedPlaybackFile.getPlaybackHandler().isPlaying())
			positionedPlaybackFile.getPlaybackHandler().promisePlayback();
	}

	@Override
	public void setVolume(float volume) {
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
			if (observer != null)
				observer.onComplete();

			isCompleted = true;
			return;
		}

		preparingPlaybackFile
			.then(VoidFunc.running(this::startFilePlayback))
			.error(VoidFunc.running(this::handlePlaybackException));
	}

	private void startFilePlayback(@NotNull PositionedPlaybackFile positionedPlaybackFile) {

		this.positionedPlaybackFile = positionedPlaybackFile;

		final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

		playbackHandler.setVolume(volume);
		playbackHandler
			.promisePlayback()
			.then(VoidFunc.running(this::closeAndStartNextFile))
			.error(VoidFunc.running(this::handlePlaybackException));

		previousPlaybackFileChanges.add(positionedPlaybackFile);
		if (observer != null)
			observer.onNext(positionedPlaybackFile);
	}

	private void closeAndStartNextFile(IPlaybackHandler playbackHandler) {
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

		if (observer != null)
			observer.onError(exception);
	}

	@Override
	public void close() throws IOException {
		haltPlayback();
	}

	@Override
	public void cancel() {
		handlePlaybackException(new CancellationException("Playlist playback was cancelled"));
	}

	@Override
	protected void subscribeActual(Observer<? super PositionedPlaybackFile> observer) {
		Stream.of(previousPlaybackFileChanges).forEach(observer::onNext);
		if (isCompleted)
			observer.onComplete();

		this.observer = observer;
	}
}
