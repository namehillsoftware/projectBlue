package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenAnInitializedMaxVolumeModifier;


import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSettingTheMaxVolume {
	private static FakeBufferingPlaybackHandler playbackHandler;

	@BeforeClass
	public static void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier = new PlaybackHandlerMaxVolumeModifier(playbackHandler, .58f);
		playbackHandlerMaxVolumeModifier.setMaxFileVolume(.8f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsCorrectlySet() {
		assertThat(playbackHandler.getVolume()).isEqualTo(.464f);
	}
}
