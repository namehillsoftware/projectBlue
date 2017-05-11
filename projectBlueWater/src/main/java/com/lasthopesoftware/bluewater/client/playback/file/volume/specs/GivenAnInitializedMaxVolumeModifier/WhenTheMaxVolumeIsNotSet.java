package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenAnInitializedMaxVolumeModifier;


import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheMaxVolumeIsNotSet {
	private static FakeBufferingPlaybackHandler playbackHandler;

	@BeforeClass
	public static void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		new PlaybackHandlerMaxVolumeModifier(playbackHandler, .58f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsCorrectlySet() {
		assertThat(playbackHandler.getVolume()).isEqualTo(.58f);
	}
}
