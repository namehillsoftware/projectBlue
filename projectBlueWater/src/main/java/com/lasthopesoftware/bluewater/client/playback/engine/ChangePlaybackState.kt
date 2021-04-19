package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

interface ChangePlaybackState {
	fun startPlaylist(playlist: MutableList<ServiceFile>, playlistPosition: Int, filePosition: Duration): Promise<Unit>
	fun resume(): Promise<Unit>
	fun pause(): Promise<Unit>
}
