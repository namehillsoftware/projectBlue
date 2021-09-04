package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenASavedExoPlayerPlaybackEngineType;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.lasthopesoftware.resources.FakeMessageBus;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingThePlaybackEngineType extends AndroidContext {

	private PlaybackEngineType playbackEngineType;

	@Override
	public void before() throws ExecutionException, InterruptedException {
		final SharedPreferences sharedPreferences = PreferenceManager
			.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());

		final PlaybackEngineTypeSelectionPersistence playbackEngineTypeSelectionPersistence =
			new PlaybackEngineTypeSelectionPersistence(
				sharedPreferences,
				new PlaybackEngineTypeChangedBroadcaster(new FakeMessageBus(ApplicationProvider.getApplicationContext())));

		playbackEngineTypeSelectionPersistence.selectPlaybackEngine(PlaybackEngineType.ExoPlayer);

		final SelectedPlaybackEngineTypeAccess selectedPlaybackEngineTypeAccess =
			new SelectedPlaybackEngineTypeAccess(
				sharedPreferences,
				Promise::empty);

		playbackEngineType = new FuturePromise<>(selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType()).get();
	}

	@Test
	public void thenThePlaybackEngineTypeIsExoPlayer() {
		assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer);
	}
}
