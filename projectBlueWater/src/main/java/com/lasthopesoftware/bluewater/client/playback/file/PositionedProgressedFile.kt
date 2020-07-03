package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile

data class PositionedProgressedFile(
	val playlistProgress: Int,
	val fileProgress: Long,
	val serviceFile: ServiceFile)
