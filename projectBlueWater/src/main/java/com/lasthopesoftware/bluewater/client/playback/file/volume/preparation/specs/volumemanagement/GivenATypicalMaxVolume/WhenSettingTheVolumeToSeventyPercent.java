package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.volumemanagement.GivenATypicalMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class WhenSettingTheVolumeToSeventyPercent {

	private static EmptyFileVolumeManager volumeManager;
	private static float returnedVolume;

	@BeforeClass
	public static void before() {
		volumeManager = new EmptyFileVolumeManager();

		final MaxFileVolumeManager maxFileVolumeManager = new MaxFileVolumeManager(volumeManager, 1);
		maxFileVolumeManager.setMaxFileVolume(.9f);
		returnedVolume = maxFileVolumeManager.setVolume(.7f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsSetToTheCorrectVolume() {
		assertThat(volumeManager.getVolume()).isCloseTo(.63f, offset(.00001f));
	}

	@Test
	public void thenTheReturnedVolumeIsSetToTheCorrectVolume() {
		assertThat(returnedVolume).isCloseTo(.63f, offset(.00001f));
	}
}
