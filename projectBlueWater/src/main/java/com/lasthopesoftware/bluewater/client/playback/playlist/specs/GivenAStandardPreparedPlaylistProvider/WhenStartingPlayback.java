package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenStartingPlayback {

	private List<PositionedPlaybackFile> positionedPlaybackFiles;

	@Before
	public void before() {
		PlayableFile playbackHandler = mock(PlayableFile.class);
		when(playbackHandler.promisePlayback()).thenReturn(new Promise<>(mock(PlayableFile.class)));

		final Promise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlaybackFile(0, playbackHandler, new ServiceFile(1)));

		final PreparedPlayableFileQueue preparedPlaybackFileQueue = mock(PreparedPlayableFileQueue.class);

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
