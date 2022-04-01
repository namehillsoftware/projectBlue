package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenAPlaybackEngineTypeSelectionBroadcaster;

import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster;
import com.lasthopesoftware.resources.FakeMessageBus;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenBroadcastingTheEngineType extends AndroidContext {

	private static final FakeMessageBus fakeMessageSender = new FakeMessageBus(ApplicationProvider.getApplicationContext());

	@Before
	public void before() {
		new PlaybackEngineTypeChangedBroadcaster(fakeMessageSender)
			.broadcastPlaybackEngineTypeChanged(PlaybackEngineType.ExoPlayer);
	}

	@Test
	public void thenTheExoPlayerSelectionIsBroadcast() {
		assertThat(fakeMessageSender.getRecordedIntents().stream().findFirst().get().getStringExtra(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeKey)).isEqualTo(PlaybackEngineType.ExoPlayer.name());
	}
}
