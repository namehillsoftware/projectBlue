package com.lasthopesoftware.bluewater.client.playback.file.preparation

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

fun interface PlayableFilePreparationSource {
	fun promisePreparedPlaybackFile(libraryId: LibraryId, serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile?>
}
