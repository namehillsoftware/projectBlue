package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenATypicalMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackFileVolumeController;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSettingTheVolumeToOne {

	private static IPlaybackHandler playbackHandler;
	private static float returnedVolume;

	@BeforeClass
	public static void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final PlaybackFileVolumeController playbackFileVolumeController = new PlaybackFileVolumeController(playbackHandler, .8f);
		returnedVolume = playbackFileVolumeController.setVolume(1f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsSetToTheMaxVolume() {
		assertThat(playbackHandler.getVolume()).isEqualTo(.8f);
	}

	@Test
	public void thenTheReturnedVolumeIsSetToTheMaxVolume() {
		assertThat(returnedVolume).isEqualTo(.8f);
	}
}
