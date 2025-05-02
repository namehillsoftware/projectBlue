package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels

import org.joda.time.Duration

interface StoreNowPlayingDisplaySettings {
	var isScreenOnDuringPlayback: Boolean
	val screenControlVisibilityTime: Duration
}
