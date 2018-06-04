package com.lasthopesoftware.bluewater.client.playback.engine.selection.specs.GivenASavedMediaPlayerPlaybackEngineType;

import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess;
import com.namehillsoftware.handoff.promises.Promise;

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
		PreferenceManager
			.getDefaultSharedPreferences(RuntimeEnvironment.application).edit()
			.putString(ApplicationConstants.PreferenceConstants.playbackEngine, "MediaPlayer")
			.apply();

		final SelectedPlaybackEngineTypeAccess selectedPlaybackEngineTypeAccess =
			new SelectedPlaybackEngineTypeAccess(
				RuntimeEnvironment.application,
				() -> new Promise<>(PlaybackEngineType.ExoPlayer));

		playbackEngineType = selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType();
	}

	@Test
	public void thenThePlaybackEngineTypeIsExoPlayer() {
		assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer);
	}
}
