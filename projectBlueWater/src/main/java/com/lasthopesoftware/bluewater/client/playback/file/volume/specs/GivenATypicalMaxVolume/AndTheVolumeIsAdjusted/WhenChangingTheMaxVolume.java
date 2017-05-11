package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenATypicalMaxVolume.AndTheVolumeIsAdjusted;


import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class WhenChangingTheMaxVolume {
	private static FakeBufferingPlaybackHandler playbackHandler;

	@BeforeClass
	public static void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier = new PlaybackHandlerMaxVolumeModifier(playbackHandler, .58f);
		playbackHandlerMaxVolumeModifier.setMaxFileVolume(.8f);
		playbackHandlerMaxVolumeModifier.setVolume(.23f);
		playbackHandlerMaxVolumeModifier.setMaxFileVolume(.47f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsCorrectlySet() {
		assertThat(playbackHandler.getVolume()).isCloseTo(.1081f, offset(.001f));
	}
}
