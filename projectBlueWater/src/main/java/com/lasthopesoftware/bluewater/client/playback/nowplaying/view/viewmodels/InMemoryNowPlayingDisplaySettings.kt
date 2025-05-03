package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels

import org.joda.time.Duration

class InMemoryNowPlayingDisplaySettings : StoreNowPlayingDisplaySettings {
	override var isScreenOnDuringPlayback = false
	override val screenControlVisibilityTime: Duration by lazy { Duration.standardSeconds(5) }
}
