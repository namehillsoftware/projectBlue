package com.lasthopesoftware.bluewater.client.playback.engine.preferences.specs.GivenASavedMediaPlayerPlaybackEngineType;

import com.lasthopesoftware.bluewater.client.playback.engine.preferences.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.PlaybackEngineTypeSelectionPersistence;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.SelectedPlaybackEngineTypeAccess;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.broadcast.PlaybackEngineTypeChangedBroadcaster;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class WhenGettingThePlaybackEngineType {

	private PlaybackEngineType playbackEngineType;

	@Before
	public void before() {
		final PlaybackEngineTypeSelectionPersistence playbackEngineTypeSelectionPersistence =
			new PlaybackEngineTypeSelectionPersistence(
				RuntimeEnvironment.application,
				mock(PlaybackEngineTypeChangedBroadcaster.class));

		playbackEngineTypeSelectionPersistence.selectPlaybackEngine(PlaybackEngineType.MediaPlayer);

		final SelectedPlaybackEngineTypeAccess selectedPlaybackEngineTypeAccess =
			new SelectedPlaybackEngineTypeAccess(RuntimeEnvironment.application);

		playbackEngineType = selectedPlaybackEngineTypeAccess.getSelectedPlaybackEngineType();
	}

	@Test
	public void thenThePlaybackEngineTypeIsMediaPlayer() {
		assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.MediaPlayer);
	}
}
