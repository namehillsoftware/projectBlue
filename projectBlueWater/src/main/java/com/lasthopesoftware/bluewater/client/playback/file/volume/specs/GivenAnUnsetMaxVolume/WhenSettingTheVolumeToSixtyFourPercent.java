package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenAnUnsetMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class WhenSettingTheVolumeToSixtyFourPercent {

	private static PlayableFile playbackHandler;
	private static float returnedVolume;

	@BeforeClass
	public static void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier = new PlaybackHandlerMaxVolumeModifier(playbackHandler, 1);
		returnedVolume = playbackHandlerMaxVolumeModifier.setVolume(.64f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsSetToTheCorrectVolume() {
		assertThat(playbackHandler.getVolume()).isCloseTo(.64f, offset(.00001f));
	}

	@Test
	public void thenTheReturnedVolumeIsSetToTheCorrectVolume() {
		assertThat(returnedVolume).isCloseTo(.64f, offset(.00001f));
	}
}
