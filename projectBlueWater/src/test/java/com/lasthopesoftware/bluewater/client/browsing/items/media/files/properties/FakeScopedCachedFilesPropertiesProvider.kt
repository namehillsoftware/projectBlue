package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise
import java.util.*

class FakeScopedCachedFilesPropertiesProvider : ProvideScopedFileProperties {
    private val cachedFileProperties: MutableMap<ServiceFile, Map<String, String>> = HashMap()
    override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> {
        return try {
            Promise(cachedFileProperties[serviceFile])
        } catch (e: Throwable) {
            Promise(e)
        }
    }

    fun addFilePropertiesToCache(serviceFile: ServiceFile, fileProperties: Map<String, String>) {
        cachedFileProperties[serviceFile] = fileProperties
    }
}
