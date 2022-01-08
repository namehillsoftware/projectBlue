package com.lasthopesoftware.bluewater.client.playback.engine.events

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

fun interface OnPlaylistReset {
    fun onPlaylistReset(positionedFile: PositionedFile)
}
