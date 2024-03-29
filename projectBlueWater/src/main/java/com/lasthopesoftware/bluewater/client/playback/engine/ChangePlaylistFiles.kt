package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.namehillsoftware.handoff.promises.Promise

interface ChangePlaylistFiles {
	fun addFile(serviceFile: ServiceFile): Promise<NowPlaying?>
	fun removeFileAtPosition(position: Int): Promise<NowPlaying?>
	fun moveFile(position: Int, newPosition: Int): Promise<NowPlaying?>
	fun clearPlaylist(): Promise<NowPlaying?>
}
