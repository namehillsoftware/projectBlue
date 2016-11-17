package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.test.GivenAStandardPreparedPlaylistProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaylistPlayback;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 11/12/16.
 */

public class WhenStartingPlayback {

	private IPlaybackHandler playbackHandler;

	@Before
	public void before() {
		playbackHandler = mock(IPlaybackHandler.class);

		final IPromise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new ExpectedPromise<>(() -> new PositionedPlaybackFile(0, playbackHandler, new File(1)));

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
		verify(playbackHandler, times(1)).promisePlayback();
	}
}
