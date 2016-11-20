package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.test.GivenAStandardPreparedPlaylistProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaylistPlayback;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;

/**
 * Created by david on 11/12/16.
 */

public class WhenStartingPlayback {

	private IPlaybackHandler playbackHandler;
	private PlaylistPlayback playlistPlayback;

	@Before
	public void before() {
		playbackHandler = mock(IPlaybackHandler.class);

		final IPromise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new ExpectedPromise<>(() -> new PositionedPlaybackFile(0, playbackHandler, new File(1)));

		playlistPlayback =
			new PlaylistPlayback(new IPreparedPlaybackFileQueue() {
				@Override
				public IPromise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(int preparedAt) {
					return positionedPlaybackHandlerContainer;
				}

				@Override
				public void close() throws IOException {

				}
			}, 0);
	}

	@Test
	public void thenPlaybackIsBegun() {
		Assert.assertNotNull(playlistPlayback.observePlaybackChanges().first());
	}
}
