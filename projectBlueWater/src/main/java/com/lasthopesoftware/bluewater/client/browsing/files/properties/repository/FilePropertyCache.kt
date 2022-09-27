package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

import com.google.common.cache.CacheBuilder
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder

object FilePropertyCache : IFilePropertiesContainerRepository {
	private const val maxSize = 500L

	private val propertiesCache by lazy {
		CacheBuilder.newBuilder()
			.maximumSize(maxSize)
			.build<UrlKeyHolder<ServiceFile>, ContainVersionedFileProperties>()
	}

	override fun getFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>): ContainVersionedFileProperties? =
		propertiesCache.getIfPresent(key)

	override fun putFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>, filePropertiesContainer: ContainVersionedFileProperties) {
		propertiesCache.put(key, filePropertiesContainer)
	}
}
