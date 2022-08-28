package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface ProvideNowPlayingFiles {
	val nowPlayingFile: Promise<ServiceFile?>
}
