package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;

import java.util.Arrays;
import java.util.Collection;

public class PlaybackFileStoppedScrobblerRegistration implements IConnectionDependentReceiverRegistration {

	private static final Collection<IntentFilter> intents =
		Arrays.asList(
			new IntentFilter(PlaylistEvents.onPlaylistTrackComplete),
			new IntentFilter(PlaylistEvents.onPlaylistStop),
			new IntentFilter(PlaylistEvents.onPlaylistPause));

	@Override
	public BroadcastReceiver registerWithConnectionProvider(IConnectionProvider connectionProvider) {
		return new PlaybackFileStoppedScrobbleDroidProxy(ScrobbleIntentProvider.getInstance());
	}

	@Override
	public Collection<IntentFilter> forIntents() {
		return intents;
	}

	private static class PlaybackFileStoppedScrobbleDroidProxy extends BroadcastReceiver {

		private final ScrobbleIntentProvider scrobbleIntentProvider;

		public PlaybackFileStoppedScrobbleDroidProxy(ScrobbleIntentProvider scrobbleIntentProvider) {
			this.scrobbleIntentProvider = scrobbleIntentProvider;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			context.sendBroadcast(scrobbleIntentProvider.provideScrobbleIntent(false));
		}
	}
}
