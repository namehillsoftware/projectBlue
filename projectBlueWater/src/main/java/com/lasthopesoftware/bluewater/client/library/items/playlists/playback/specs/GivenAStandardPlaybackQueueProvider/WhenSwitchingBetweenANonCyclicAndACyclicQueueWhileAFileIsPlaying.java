package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPlaybackQueueProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;

import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 12/22/16.
 */

public class WhenSwitchingBetweenANonCyclicAndACyclicQueueWhileAFileIsPlaying {

	private IPlaybackHandler playbackHandler;

	@Test
	public void thenThePlaybackOfTheCurrentFileIsNeverPaused() {
		verify(this.playbackHandler, times(0)).pause();
	}

	@Test
	public void thenTheCurrentPlaybackHandlerIsNeverClosed() throws IOException {
		verify(this.playbackHandler, times(0)).close();
	}
}
