package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingMessage
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus
import org.koin.dsl.module

val appModule = module {
	scope<NowPlayingActivity> {
		scoped { TypedMessageBus<NowPlayingMessage>() }
	}
}
