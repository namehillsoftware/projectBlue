package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder

class FakeFilePropertiesContainer : IFilePropertiesContainerRepository {
    private val storage = HashMap<UrlKeyHolder<ServiceFile>, FilePropertiesContainer>()
    override fun getFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>): FilePropertiesContainer? {
        return storage[key]
    }

    override fun putFilePropertiesContainer(
        key: UrlKeyHolder<ServiceFile>,
        filePropertiesContainer: FilePropertiesContainer
    ) {
        storage[key] = filePropertiesContainer
    }
}
