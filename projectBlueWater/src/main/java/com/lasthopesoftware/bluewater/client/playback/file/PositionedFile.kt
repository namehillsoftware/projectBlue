package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile

data class PositionedFile(val playlistPosition: Int, val serviceFile: ServiceFile) : Comparable<PositionedFile> {
	override fun compareTo(other: PositionedFile): Int {
		val playlistComparison = playlistPosition - other.playlistPosition
		return if (playlistComparison != 0) playlistComparison else serviceFile.compareTo(other.serviceFile)
	}

	override fun toString(): String = "PositionedFile(playlistPosition=$playlistPosition, serviceFile=$serviceFile)"
}
