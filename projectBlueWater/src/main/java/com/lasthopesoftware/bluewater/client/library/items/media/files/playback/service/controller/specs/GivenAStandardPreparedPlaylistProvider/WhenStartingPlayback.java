package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.specs.GivenAStandardPreparedPlaylistProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaylistPlayer;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 11/12/16.
 */

public class WhenStartingPlayback {

	private IPlaybackHandler playbackHandler;
	private Collection<PositionedPlaybackFile> positionedPlaybackFiles;

	@Before
	public void before() {
		playbackHandler = mock(IPlaybackHandler.class);
		when(playbackHandler.promisePlayback()).thenReturn(new ExpectedPromise<>(() -> mock(IPlaybackHandler.class)));

		final IPromise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new ExpectedPromise<>(() -> new PositionedPlaybackFile(0, playbackHandler, new File(1)));

		final IPreparedPlaybackFileQueue preparedPlaybackFileQueue = mock(IPreparedPlaybackFileQueue.class);

		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(null);

		new PlaylistPlayer(preparedPlaybackFileQueue, 0)
			.then(positionedPlaybackFiles -> this.positionedPlaybackFiles = positionedPlaybackFiles);
	}

	@Test
	public void thenThePlaybackCountIsCorrect() {
		assertThat(this.positionedPlaybackFiles.size()).isEqualTo(5);
	}
}
