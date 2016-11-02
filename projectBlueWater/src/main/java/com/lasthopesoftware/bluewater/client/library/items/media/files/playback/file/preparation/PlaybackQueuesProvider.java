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

	public PlaybackQueuesProvider(IFileUriProvider fileUriProvider, IPlaybackInitialization<MediaPlayer> mediaPlayerInitializer) {
		this.fileUriProvider = fileUriProvider;
		this.mediaPlayerInitializer = mediaPlayerInitializer;
	}

	@Override
	public IPreparedPlaybackFileProvider getQueue(List<IFile> playlist, int startingAt, boolean isCyclical) {
		final List<IFile> truncatedList = Stream.of(playlist).skip(startingAt - 1).collect(Collectors.toList());

		if (!isCyclical)
			return new QueuedMediaPlayerProvider(truncatedList, fileUriProvider, mediaPlayerInitializer);

		truncatedList.addAll(truncatedList.subList(0, startingAt - 1));

		return new CyclicalQueuedMediaPlayerProvider(truncatedList, fileUriProvider, mediaPlayerInitializer);
	}
}
