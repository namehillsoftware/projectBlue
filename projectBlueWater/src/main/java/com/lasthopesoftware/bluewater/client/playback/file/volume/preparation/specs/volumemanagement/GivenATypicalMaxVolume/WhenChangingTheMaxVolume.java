package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.volumemanagement.GivenATypicalMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenChangingTheMaxVolume {
	private static EmptyFileVolumeManager volumeManager;

	@BeforeClass
	public static void before() {
		volumeManager = new EmptyFileVolumeManager();

		final MaxFileVolumeManager maxFileVolumeManager = new MaxFileVolumeManager(volumeManager, .58f);
		maxFileVolumeManager.setMaxFileVolume(.8f);
		maxFileVolumeManager.setMaxFileVolume(.47f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsCorrectlySet() {
		assertThat(volumeManager.getVolume()).isEqualTo(.2726f);
	}
}
