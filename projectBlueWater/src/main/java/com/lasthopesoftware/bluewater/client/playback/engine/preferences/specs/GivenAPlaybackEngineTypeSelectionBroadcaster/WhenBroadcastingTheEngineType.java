package com.lasthopesoftware.bluewater.client.playback.engine.preferences.specs.GivenAPlaybackEngineTypeSelectionBroadcaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.playback.engine.preferences.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.broadcast.PlaybackEngineTypeChangedBroadcaster;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WhenBroadcastingTheEngineType {

	private String broadcastEngineType;

	@Before
	public void before() {
		LocalBroadcastManager.getInstance(RuntimeEnvironment.application)
			.registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					broadcastEngineType =
						intent.getStringExtra(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeKey);
				}
			}, new IntentFilter(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeChanged));

		new PlaybackEngineTypeChangedBroadcaster(RuntimeEnvironment.application)
			.broadcastPlaybackEngineTypeChanged(PlaybackEngineType.ExoPlayer);
	}

	@Test
	public void thenTheExoPlayerSelectionIsBroadcast() {
		assertThat(broadcastEngineType).isEqualTo(PlaybackEngineType.ExoPlayer.name());
	}
}
