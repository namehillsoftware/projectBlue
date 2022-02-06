package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile

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

	val playingFile: ServiceFile?
		get() =
			if (playlistPosition > -1 && playlistPosition < playlist.size) playlist[playlistPosition]
			else null

    constructor(playlistPosition: Int, filePosition: Long, isRepeating: Boolean)
		: this(emptyList(), playlistPosition, filePosition, isRepeating)
}
