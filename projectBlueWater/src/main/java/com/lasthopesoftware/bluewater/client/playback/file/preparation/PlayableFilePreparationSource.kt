package com.lasthopesoftware.bluewater.client.playback.file.preparation

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

fun interface PlayableFilePreparationSource {
	fun promisePreparedPlaybackFile(serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile>
}
