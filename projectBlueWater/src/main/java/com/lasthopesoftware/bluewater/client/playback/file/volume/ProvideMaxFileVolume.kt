package com.lasthopesoftware.bluewater.client.playback.file.volume

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideMaxFileVolume {
    fun promiseMaxFileVolume(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Float>
}
