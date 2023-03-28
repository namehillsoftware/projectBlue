package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble;


import android.content.Intent;

class ScrobbleIntentProvider {

	private static ScrobbleIntentProvider scrobbleIntentProvider = new ScrobbleIntentProvider();

	static ScrobbleIntentProvider getInstance() {
		return scrobbleIntentProvider;
	}

	private ScrobbleIntentProvider() {}

	Intent provideScrobbleIntent(boolean isPlaying) {
		final Intent scrobbleDroidIntent = new Intent(SCROBBLE_DROID_INTENT);
		scrobbleDroidIntent.putExtra("playing", isPlaying);

		return scrobbleDroidIntent;
	}

	private static final String SCROBBLE_DROID_INTENT = "net.jjc1138.android.scrobbler.action.MUSIC_STATUS";
}
