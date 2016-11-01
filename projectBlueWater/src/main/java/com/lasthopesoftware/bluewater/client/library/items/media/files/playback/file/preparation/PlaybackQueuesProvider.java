package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.promises.IPromise;

import java.io.IOException;
import java.util.List;

/**
 * Created by david on 11/1/16.
 */
public class PlaybackQueuesProvider implements IProvidePlaybackQueues, IPreparedPlaybackFileProvider {

	private final List<IFile> playlist;
	private final IFileUriProvider fileUriProvider;
	private final IPlaybackInitialization<MediaPlayer> mediaPlayerInitializer;

	private IPreparedPlaybackFileProvider internalQueue;
	private int listPosition;


	public PlaybackQueuesProvider(List<IFile> playlist, IFileUriProvider fileUriProvider, IPlaybackInitialization<MediaPlayer> mediaPlayerInitializer) {
		this.playlist = playlist;
		this.fileUriProvider = fileUriProvider;
		this.mediaPlayerInitializer = mediaPlayerInitializer;
	}

	@Override
	public IPreparedPlaybackFileProvider getQueue() {
		return getQueue(listPosition);
	}

	@Override
	public IPreparedPlaybackFileProvider getRepeatingQueue() {
		return getRepeatingQueue(listPosition);
	}

	@Override
	public IPreparedPlaybackFileProvider getQueue(int startingAt) {
		listPosition = startingAt;

		internalQueue = new QueuedMediaPlayerProvider(getTruncatedList(startingAt), fileUriProvider, mediaPlayerInitializer);
		return this;
	}

	@Override
	public IPreparedPlaybackFileProvider getRepeatingQueue(int startingAt) {
		listPosition = startingAt;

		final List<IFile> positionalPlaylist = getTruncatedList(startingAt);
		positionalPlaylist.addAll(playlist.subList(0, listPosition - 1));

		internalQueue = new CyclicalQueuedMediaPlayerProvider(positionalPlaylist, fileUriProvider, mediaPlayerInitializer);
		return this;
	}

	private List<IFile> getTruncatedList(int startingAt) {
		return Stream.of(playlist).skip(startingAt - 1).collect(Collectors.toList());
	}

	@Override
	public IPromise<IPlaybackHandler> promiseNextPreparedPlaybackFile(int preparedAt) {
		if (internalQueue == null) return null;

		final IPromise<IPlaybackHandler> nextPreparedPlaybackFilePromise =
			promiseNextPreparedPlaybackFile(preparedAt);

		nextPreparedPlaybackFilePromise
			.then(playbackHandler -> { ++listPosition; });

		return nextPreparedPlaybackFilePromise;
	}

	@Override
	public void close() throws IOException {
		if (internalQueue != null)
			internalQueue.close();
	}
}
