package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenATypicalPreferenceManagerAndBroadcaster;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster;
import com.lasthopesoftware.resources.FakeMessageSender;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenPersistingTheSelectedPlaybackEngine extends AndroidContext {
	private static final FakeMessageSender fakeMessageSender = new FakeMessageSender();

	private String persistedEngineType;

	@Override
	public void before() throws InterruptedException {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final SharedPreferences sharedPreferences = PreferenceManager
			.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());
		sharedPreferences
			.registerOnSharedPreferenceChangeListener((sp, key) -> {
				persistedEngineType = sp.getString(key, null);
				countDownLatch.countDown();
			});

		new PlaybackEngineTypeSelectionPersistence(
			sharedPreferences,
			new PlaybackEngineTypeChangedBroadcaster(fakeMessageSender))
				.selectPlaybackEngine(PlaybackEngineType.ExoPlayer);

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenTheExoPlayerSelectionIsBroadcast() {
		assertThat(fakeMessageSender.getRecordedIntents().stream().findFirst().get().getStringExtra(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeKey)).isEqualTo(PlaybackEngineType.ExoPlayer.name());
	}

	@Test
	public void thenTheExoPlayerSelectionIsPersisted() {
		assertThat(persistedEngineType).isEqualTo(PlaybackEngineType.ExoPlayer.name());
	}
}
