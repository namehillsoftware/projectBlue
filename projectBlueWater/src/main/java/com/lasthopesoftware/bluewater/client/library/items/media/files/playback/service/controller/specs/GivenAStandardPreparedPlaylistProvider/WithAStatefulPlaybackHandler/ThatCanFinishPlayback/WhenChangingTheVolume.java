package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.StatefulPlaybackHandler;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 12/7/16.
 */

public class WhenChangingTheVolume {

	private StatefulPlaybackHandler playbackHandlerUnderTest;

	@Before
	public void before() {
		final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
		playbackHandlerUnderTest = new StatefulPlaybackHandler();

		final IPromise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new ExpectedPromise<>(() -> new PositionedPlaybackFile(0, playbackHandler, new File(1)));

		final IPromise<PositionedPlaybackFile> secondPositionedPlaybackHandlerContainer =
			new ExpectedPromise<>(() -> new PositionedPlaybackFile(0, playbackHandlerUnderTest, new File(1)));

		final IPreparedPlaybackFileQueue preparedPlaybackFileQueue = mock(IPreparedPlaybackFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(secondPositionedPlaybackHandlerContainer);

		final IPlaylistPlayer playlistPlayback = new PlaylistPlayer(preparedPlaybackFileQueue, 0, null);

		playlistPlayback.setVolume(0.8f);

		playbackHandler.resolve();
	}

	@Test
	public void thenTheVolumeIsChanged() {
		assertThat(playbackHandlerUnderTest.getVolume()).isEqualTo(0.8f);
	}
}
