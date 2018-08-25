package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.specs.GivenATypicalExoPlayer;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.ActiveExoPlaylistPlayer;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class WhenCreatingAnActiveExoPlayer {

	private static final PlaybackReportingExoPlayer exoPlayer = mock(PlaybackReportingExoPlayer.class);

	@BeforeClass
	public static void before() {
		final ActiveExoPlaylistPlayer exoPlaylistPlayer = new ActiveExoPlaylistPlayer(exoPlayer);
	}

	@Test
	public void thenPlaybackIsStarted() {
		assertThat(exoPlayer.getPlayWhenReady()).isTrue();
	}

	private abstract static class PlaybackReportingExoPlayer implements ExoPlayer {

		private boolean playWhenReady;

		@Override
		public final void setPlayWhenReady(boolean playWhenReady) {
			this.playWhenReady = playWhenReady;
		}

		@Override
		public final boolean getPlayWhenReady() {
			return playWhenReady;
		}
	}
}
