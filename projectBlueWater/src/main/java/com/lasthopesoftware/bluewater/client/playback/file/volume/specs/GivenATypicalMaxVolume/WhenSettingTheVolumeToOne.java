package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenATypicalMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSettingTheVolumeToOne {

	private static EmptyFileVolumeManager volumeManager;
	private static float returnedVolume;

	@BeforeClass
	public static void before() {
		volumeManager = new EmptyFileVolumeManager();

		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier = new PlaybackHandlerMaxVolumeModifier(volumeManager, 1);
		playbackHandlerMaxVolumeModifier.setMaxFileVolume(.8f);
		returnedVolume = playbackHandlerMaxVolumeModifier.setVolume(1f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsSetToTheMaxVolume() {
		assertThat(volumeManager.getVolume()).isEqualTo(.8f);
	}

	@Test
	public void thenTheReturnedVolumeIsSetToTheMaxVolume() {
		assertThat(returnedVolume).isEqualTo(.8f);
	}
}
