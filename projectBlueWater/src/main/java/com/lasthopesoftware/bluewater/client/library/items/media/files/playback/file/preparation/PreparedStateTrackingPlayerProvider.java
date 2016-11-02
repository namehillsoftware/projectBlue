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

	private int position;

	public PreparedStateTrackingPlayerProvider(int startingPosition, int size, IPreparedPlaybackFileProvider internalPlaybackFileProvider) {
		this.position = startingPosition;
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
				++position;
				position %= size;
			});

		return internalPreparedPlaybackPromise;
	}

	@Override
	public void close() throws IOException {
		internalPlaybackFileProvider.close();
	}

	@Override
	public int getPosition() {
		return position;
	}
}
