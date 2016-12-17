package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaylistPlayer;
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

	private StatefulPlaybackHandler playbackHandler;

	@Before
	public void before() {
		playbackHandler = new StatefulPlaybackHandler();
		playbackHandler.promisePlayback();

		final IPromise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new ExpectedPromise<>(() -> new PositionedPlaybackFile(0, playbackHandler, new File(1)));

		final IPreparedPlaybackFileQueue preparedPlaybackFileQueue = mock(IPreparedPlaybackFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer);

		final IPlaylistPlayer playlistPlayback = new PlaylistPlayer(preparedPlaybackFileQueue, 0, null);

		playlistPlayback.setVolume(0.8f);
	}

	@Test
	public void thenTheVolumeIsChanged() {
		assertThat(playbackHandler.getVolume()).isEqualTo(0.8f);
	}
}
