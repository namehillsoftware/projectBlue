package com.lasthopesoftware.bluewater

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.NowPlayingPlaylistFragment
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.NowPlayingPlaylistMessage
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus
import org.koin.dsl.module

val appModule = module {
	single { MessageBus(LocalBroadcastManager.getInstance(get())) }

	scope<NowPlayingPlaylistFragment> {
		scoped { TypedMessageBus<NowPlayingPlaylistMessage>() }
	}
}
