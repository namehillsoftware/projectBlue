package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.buffering.BufferingPlaybackFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class FakeFilePreparer(private val playableFile: PlayableFile, private val bufferingPlaybackFile: BufferingPlaybackFile) : PlayableFilePreparationSource {
	override fun promisePreparedPlaybackFile(libraryId: LibraryId, serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile?> =
		Promise(PreparedPlayableFile(
			playableFile,
			NoTransformVolumeManager(),
			bufferingPlaybackFile))
}
