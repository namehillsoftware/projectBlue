package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile

/**
 * Created by david on 1/29/17.
 */
class NowPlaying constructor(
	@JvmField
    var playlistPosition: Int,
	@JvmField
    var filePosition: Long,
	@JvmField
    var isRepeating: Boolean
) {
    @JvmField
	var playlist: List<ServiceFile> = emptyList()

	val playingFile: ServiceFile?
		get() =
			if (playlistPosition > -1 && playlistPosition < playlist.size) playlist[playlistPosition]
			else null


    constructor(
        playlist: List<ServiceFile>,
        playlistPosition: Int,
        filePosition: Long,
        isRepeating: Boolean
    ) : this(playlistPosition, filePosition, isRepeating) {
        this.playlist = playlist
    }
}
