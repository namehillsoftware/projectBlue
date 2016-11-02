package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;

import java.util.List;

/**
 * Created by david on 11/1/16.
 */
public class PlaybackQueuesProvider implements IProvidePlaybackQueues {

	private final IFileUriProvider fileUriProvider;
	private final IPlaybackInitialization<MediaPlayer> mediaPlayerInitializer;
	private final List<IFile> playlist;

	private IPreparedPlayerStateTracker internalStateTrackingPlayerProvider;

	public PlaybackQueuesProvider(List<IFile> playlist, IFileUriProvider fileUriProvider, IPlaybackInitialization<MediaPlayer> mediaPlayerInitializer) {
		this.playlist = playlist;
		this.fileUriProvider = fileUriProvider;
		this.mediaPlayerInitializer = mediaPlayerInitializer;
	}

	@Override
	public IPreparedPlaybackFileProvider getQueue(boolean isCyclical) {
		return
			getQueue(
				isCyclical,
				internalStateTrackingPlayerProvider != null ? internalStateTrackingPlayerProvider.getPosition() : 0);
	}

	@Override
	public IPreparedPlaybackFileProvider getQueue(boolean isCyclical, int startingAt) {
		final List<IFile> truncatedList = Stream.of(playlist).skip(startingAt - 1).collect(Collectors.toList());

		if (!isCyclical) {
			internalStateTrackingPlayerProvider =
				new PreparedStateTrackingPlayerProvider(
					startingAt,
					playlist.size(),
					new QueuedMediaPlayerProvider(truncatedList, fileUriProvider, mediaPlayerInitializer));
			return internalStateTrackingPlayerProvider;
		}

		truncatedList.addAll(truncatedList.subList(0, startingAt - 1));

		internalStateTrackingPlayerProvider =
			new PreparedStateTrackingPlayerProvider(
				startingAt,
				playlist.size(),
				new CyclicalQueuedMediaPlayerProvider(truncatedList, fileUriProvider, mediaPlayerInitializer));

		return internalStateTrackingPlayerProvider;
	}
}
