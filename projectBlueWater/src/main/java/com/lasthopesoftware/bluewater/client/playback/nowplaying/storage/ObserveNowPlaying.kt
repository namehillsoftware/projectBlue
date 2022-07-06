package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import kotlinx.coroutines.flow.StateFlow

interface ObserveNowPlaying {
	val nowPlayingState: StateFlow<PositionedFile?>
}
