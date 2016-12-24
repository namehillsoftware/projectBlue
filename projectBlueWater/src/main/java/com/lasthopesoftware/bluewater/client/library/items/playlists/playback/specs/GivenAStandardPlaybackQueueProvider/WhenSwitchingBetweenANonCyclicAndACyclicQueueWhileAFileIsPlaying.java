package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPlaybackQueueProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 12/22/16.
 */

public class WhenSwitchingBetweenANonCyclicAndACyclicQueueWhileAFileIsPlaying {

	private IPlaybackHandler playbackHandler;
	private IPlaybackHandler expectedPlaybackHandler;

	@BeforeClass
	public static void before() {

	}

	@Test
	public void thenThePlaybackOfTheCurrentFileIsNeverPaused() {
		verify(this.playbackHandler, times(0)).pause();
	}

	@Test
	public void thenTheCurrentPlaybackHandlerIsNeverClosed() throws IOException {
		verify(this.playbackHandler, times(0)).close();
	}

	@Test
	public void thenTheCurrentPlaybackHandlerIsPlaying() {
		assertThat(this.playbackHandler.isPlaying()).isTrue();
	}
	
	@Test
	public void thenThePlaybackHandlerRemainsTheSame() {
		assertThat(this.playbackHandler).isEqualTo(this.expectedPlaybackHandler);
	}
}
