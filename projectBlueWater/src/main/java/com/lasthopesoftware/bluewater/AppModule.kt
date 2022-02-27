package com.lasthopesoftware.bluewater

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingMessage
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus
import org.koin.dsl.module

val appModule = module {
	single { MessageBus(LocalBroadcastManager.getInstance(get())) }

	scope<NowPlayingActivity> {
		scoped { TypedMessageBus<NowPlayingMessage>() }
	}
}
