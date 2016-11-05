package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;

import java.io.IOException;

/**
 * Created by david on 11/1/16.
 */

public class PreparedStateTrackingPlayerProvider implements IPreparedPlayerStateTracker {

	private final IPreparedPlaybackFileProvider internalPlaybackFileProvider;
	private final int size;

	private int preparedPosition;

	public PreparedStateTrackingPlayerProvider(int startingPosition, int size, IPreparedPlaybackFileProvider internalPlaybackFileProvider) {
		this.preparedPosition = startingPosition - 1;
		this.size = size;
		this.internalPlaybackFileProvider = internalPlaybackFileProvider;
	}

	@Override
	public IPromise<IPlaybackHandler> promiseNextPreparedPlaybackFile(int preparedAt) {
		final IPromise<IPlaybackHandler> internalPreparedPlaybackPromise
			= internalPlaybackFileProvider.promiseNextPreparedPlaybackFile(preparedAt);

		if (internalPreparedPlaybackPromise == null)
			return null;

		internalPreparedPlaybackPromise
			.then(playbackHandler -> {
				preparedPosition = ++preparedPosition % size;
			});

		return internalPreparedPlaybackPromise;
	}

	@Override
	public void close() throws IOException {
		internalPlaybackFileProvider.close();
	}

	@Override
	public int getPreparedIndex() {
		return preparedPosition;
	}
}
