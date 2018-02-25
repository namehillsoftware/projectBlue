package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenAnInitializedMaxVolumeModifier;


import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSettingTheMaxVolume {
	private static EmptyFileVolumeManager volumeManager;

	@BeforeClass
	public static void before() {
		volumeManager = new EmptyFileVolumeManager();

		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier = new PlaybackHandlerMaxVolumeModifier(volumeManager, .58f);
		playbackHandlerMaxVolumeModifier.setMaxFileVolume(.8f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsCorrectlySet() {
		assertThat(volumeManager.getVolume()).isEqualTo(.464f);
	}
}
