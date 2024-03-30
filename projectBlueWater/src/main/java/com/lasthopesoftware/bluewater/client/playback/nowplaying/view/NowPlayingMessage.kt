package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import com.lasthopesoftware.bluewater.shared.messages.TypedMessage

interface NowPlayingMessage : TypedMessage {
	object ScrollToNowPlaying : NowPlayingMessage
}
