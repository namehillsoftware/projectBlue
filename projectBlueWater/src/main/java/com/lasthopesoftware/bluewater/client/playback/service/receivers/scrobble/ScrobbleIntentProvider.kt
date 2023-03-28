package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble

import android.content.Intent

object ScrobbleIntentProvider {
    fun provideScrobbleIntent(isPlaying: Boolean): Intent {
        val scrobbleDroidIntent = Intent(SCROBBLE_DROID_INTENT)
        scrobbleDroidIntent.putExtra("playing", isPlaying)
        return scrobbleDroidIntent
    }

	private const val SCROBBLE_DROID_INTENT =
		"net.jjc1138.android.scrobbler.action.MUSIC_STATUS"
}
