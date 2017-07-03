package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.messenger.promise.Promise;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 11/12/16.
 */

public class WhenStartingPlayback {

	private List<PositionedPlaybackFile> positionedPlaybackFiles;

	@Before
	public void before() {
		IPlaybackHandler playbackHandler = mock(IPlaybackHandler.class);
		when(playbackHandler.promisePlayback()).thenReturn(new Promise<>(mock(IPlaybackHandler.class)));

		final Promise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlaybackFile(0, playbackHandler, new ServiceFile(1)));

		final IPreparedPlaybackFileQueue preparedPlaybackFileQueue = mock(IPreparedPlaybackFileQueue.class);

		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(null);

		Observable.create(new PlaylistPlayer(preparedPlaybackFileQueue, mock(IPlaybackHandlerVolumeControllerFactory.class), 0))
			.toList().subscribe(positionedPlaybackFiles -> this.positionedPlaybackFiles = positionedPlaybackFiles);
	}

	@Test
	public void thenThePlaybackCountIsCorrect() {
		assertThat(this.positionedPlaybackFiles.size()).isEqualTo(5);
	}
}
