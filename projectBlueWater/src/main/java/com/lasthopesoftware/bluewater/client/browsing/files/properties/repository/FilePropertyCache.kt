package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

import com.github.benmanes.caffeine.cache.Caffeine
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.resources.executors.ThreadPools

object FilePropertyCache : IFilePropertiesContainerRepository {
	private const val maxSize = 500L

	private val propertiesCache by lazy {
		Caffeine.newBuilder()
			.executor(ThreadPools.compute)
			.maximumSize(maxSize)
			.build<UrlKeyHolder<ServiceFile>, ContainVersionedFileProperties>()
	}

	override fun getFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>): ContainVersionedFileProperties? =
		propertiesCache.getIfPresent(key)

	override fun putFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>, filePropertiesContainer: ContainVersionedFileProperties) {
		propertiesCache.put(key, filePropertiesContainer)
	}
}
