package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile

data class PositionedFile(val playlistPosition: Int, val serviceFile: ServiceFile) : Comparable<PositionedFile> {
	override fun compareTo(other: PositionedFile): Int = playlistPosition - other.playlistPosition

	override fun toString(): String = "PositionedFile(playlistPosition=$playlistPosition, serviceFile=$serviceFile)"
}
