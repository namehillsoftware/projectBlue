package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class PlaybackStartedBroadcaster {

	private final LocalBroadcastManager localBroadcastManager;

	public PlaybackStartedBroadcaster(LocalBroadcastManager localBroadcastManager) {
		this.localBroadcastManager = localBroadcastManager;
	}

	public void broadcastPlaybackStarted() {
		final Intent playbackBroadcastIntent = new Intent(PlaylistEvents.onPlaylistStart);
		localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
	}
}
