package com.lasthopesoftware.bluewater.client.playback.file

import android.os.Parcelable
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import kotlinx.parcelize.Parcelize

@Parcelize
data class PositionedFile(val playlistPosition: Int, val serviceFile: ServiceFile) : Comparable<PositionedFile>, Parcelable {
	override fun compareTo(other: PositionedFile): Int = playlistPosition - other.playlistPosition

	override fun toString(): String = "PositionedFile(playlistPosition=$playlistPosition, serviceFile=$serviceFile)"
}
