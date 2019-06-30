package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.volumemanagement.GivenAnUnsetMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSettingTheVolumeToOne {

	private static EmptyFileVolumeManager volumeManager;
	private static float returnedVolume;

	@BeforeClass
	public static void before() {
		volumeManager = new EmptyFileVolumeManager();

		final MaxFileVolumeManager maxFileVolumeManager = new MaxFileVolumeManager(volumeManager, 1);
		returnedVolume = maxFileVolumeManager.setVolume(1f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsSetToTheMaxVolume() {
		assertThat(volumeManager.getVolume()).isEqualTo(1);
	}

	@Test
	public void thenTheReturnedVolumeIsSetToTheMaxVolume() {
		assertThat(returnedVolume).isEqualTo(1);
	}
}
