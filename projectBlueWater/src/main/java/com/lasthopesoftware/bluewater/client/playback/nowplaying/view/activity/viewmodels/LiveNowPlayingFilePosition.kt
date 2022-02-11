package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import org.joda.time.Duration

class LiveNowPlayingFilePosition : GetLiveNowPlayingFilePosition {
	override val progressedFile: Pair<UrlKeyHolder<PositionedFile>, Duration>?
		get() = TODO("Not yet implemented")
}
