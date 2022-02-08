package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

class NowPlaying constructor(
	@JvmField
	var playlist: List<ServiceFile>,
	@JvmField
    var playlistPosition: Int,
	@JvmField
    var filePosition: Long,
	@JvmField
    var isRepeating: Boolean
) {

	val playingFile: PositionedFile?
		get() =
			playlistPosition.takeIf { it > -1 && it < playlist.size }?.let { PositionedFile(it, playlist[it]) }

    constructor(playlistPosition: Int, filePosition: Long, isRepeating: Boolean)
		: this(emptyList(), playlistPosition, filePosition, isRepeating)
}
