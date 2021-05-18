package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

interface SupplyQueuedPreparedFiles {
	fun promiseNextPreparedPlaybackFile(preparedAt: Duration): Promise<PositionedPlayableFile>?
}
