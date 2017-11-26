package com.lasthopesoftware.bluewater.client.playback.engine.preferences.specs.GivenAnUnconfiguredPlaybackEngine;

import com.lasthopesoftware.bluewater.client.playback.engine.preferences.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.SelectedPlaybackEngineTypeAccess;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WhenGettingThePlaybackEngineType {

	private PlaybackEngineType playbackEngineType;

	@Before
	public void before() {
		final SelectedPlaybackEngineTypeAccess selectedPlaybackEngineTypeAccess =
			new SelectedPlaybackEngineTypeAccess(RuntimeEnvironment.application);

		playbackEngineType = selectedPlaybackEngineTypeAccess.getSelectedPlaybackEngineType();
	}

	@Test
	public void thenThePlaybackEngineTypeIsMediaPlayer() {
		assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.MediaPlayer);
	}
}
