package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class FakeFilePreparer(private val playableFile: PlayableFile, private val bufferingPlaybackFile: IBufferingPlaybackFile) : PlayableFilePreparationSource {
	override fun promisePreparedPlaybackFile(serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile> =
		Promise(PreparedPlayableFile(
			playableFile,
			NoTransformVolumeManager(),
			bufferingPlaybackFile))
}
