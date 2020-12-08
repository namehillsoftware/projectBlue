package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying
import com.namehillsoftware.handoff.promises.Promise

interface ChangePlaylistFiles {
	fun addFile(serviceFile: ServiceFile): Promise<NowPlaying>
	fun removeFileAtPosition(position: Int): Promise<NowPlaying>
}
