package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenATypicalMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class WhenSettingTheVolumeToSeventyPercent {

	private static IPlaybackHandler playbackHandler;
	private static float returnedVolume;

	@BeforeClass
	public static void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier = new PlaybackHandlerMaxVolumeModifier(playbackHandler, 1);
		playbackHandlerMaxVolumeModifier.setMaxFileVolume(.9f);
		returnedVolume = playbackHandlerMaxVolumeModifier.setVolume(.7f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsSetToTheCorrectVolume() {
		assertThat(playbackHandler.getVolume()).isCloseTo(.63f, offset(.00001f));
	}

	@Test
	public void thenTheReturnedVolumeIsSetToTheCorrectVolume() {
		assertThat(returnedVolume).isCloseTo(.63f, offset(.00001f));
	}
}
