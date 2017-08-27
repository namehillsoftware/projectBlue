package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenAnUnsetMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSettingTheVolumeToOne {

	private static IPlaybackHandler playbackHandler;
	private static float returnedVolume;

	@BeforeClass
	public static void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier = new PlaybackHandlerMaxVolumeModifier(playbackHandler, 1);
		returnedVolume = playbackHandlerMaxVolumeModifier.setVolume(1f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsSetToTheMaxVolume() {
		assertThat(playbackHandler.getVolume()).isEqualTo(1);
	}

	@Test
	public void thenTheReturnedVolumeIsSetToTheMaxVolume() {
		assertThat(returnedVolume).isEqualTo(1);
	}
}
