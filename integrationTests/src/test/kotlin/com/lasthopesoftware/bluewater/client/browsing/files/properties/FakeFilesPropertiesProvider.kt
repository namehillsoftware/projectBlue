package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class FakeFilesPropertiesProvider : ProvideLibraryFileProperties {
    private val cachedFileProperties = HashMap<Pair<ServiceFile, LibraryId>, Map<String, String>>()
    override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> =
		try {
			Promise(cachedFileProperties[Pair(serviceFile, libraryId)])
		} catch (e: Throwable) {
			Promise(e)
		}

    fun addFilePropertiesToCache(serviceFile: ServiceFile, libraryId: LibraryId, fileProperties: Map<String, String>) {
        cachedFileProperties[Pair(serviceFile, libraryId)] = fileProperties
    }
}
