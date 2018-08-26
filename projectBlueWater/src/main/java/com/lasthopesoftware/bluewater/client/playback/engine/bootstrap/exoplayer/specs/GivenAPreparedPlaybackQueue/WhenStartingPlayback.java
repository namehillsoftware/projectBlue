package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.exoplayer.specs.GivenAPreparedPlaybackQueue;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.exoplayer.ExoPlayerPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.ActiveExoPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPreparedPlaybackQueueConfiguration;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class WhenStartingPlayback {

	private static IActivePlayer activePlayer;

	@BeforeClass
	public static void before() {
		final ExoPlayerPlaybackBootstrapper bootstrapper = new ExoPlayerPlaybackBootstrapper(
			() -> mock(ExoPlayer.class));

		activePlayer = bootstrapper.startPlayback(new PreparedPlayableFileQueue(
			mock(IPreparedPlaybackQueueConfiguration.class),
			mock(PlayableFilePreparationSource.class),
			mock(IPositionedFileQueue.class)), 0);
	}

	@Test
	public void thenAnActiveExoPlaylistPlayerIsReturned() {
		assertThat(activePlayer).isInstanceOf(ActiveExoPlaylistPlayer.class);
	}
}
