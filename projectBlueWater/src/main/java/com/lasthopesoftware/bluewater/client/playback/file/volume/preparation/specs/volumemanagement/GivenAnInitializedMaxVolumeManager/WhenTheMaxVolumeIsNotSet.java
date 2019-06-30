package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.volumemanagement.GivenAnInitializedMaxVolumeManager;


import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheMaxVolumeIsNotSet {
	private static EmptyFileVolumeManager volumeManager;

	@BeforeClass
	public static void before() {
		volumeManager = new EmptyFileVolumeManager();

		new MaxFileVolumeManager(volumeManager, .58f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsCorrectlySet() {
		assertThat(volumeManager.getVolume()).isEqualTo(.58f);
	}
}
