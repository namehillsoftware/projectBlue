package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.viewmodels

import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder

interface StoreFilePositionByFile {
	var progressedFile: UrlKeyHolder<PositionedProgressedFile>
}
