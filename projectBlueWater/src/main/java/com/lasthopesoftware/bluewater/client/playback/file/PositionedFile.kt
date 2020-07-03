package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile

data class PositionedFile(val playlistPosition: Int, val serviceFile: ServiceFile)
