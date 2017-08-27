package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenATypicalMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenChangingTheMaxVolume {
	private static FakeBufferingPlaybackHandler playbackHandler;

	@BeforeClass
	public static void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier = new PlaybackHandlerMaxVolumeModifier(playbackHandler, .58f);
		playbackHandlerMaxVolumeModifier.setMaxFileVolume(.8f);
		playbackHandlerMaxVolumeModifier.setMaxFileVolume(.47f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsCorrectlySet() {
		assertThat(playbackHandler.getVolume()).isEqualTo(.2726f);
	}
}
