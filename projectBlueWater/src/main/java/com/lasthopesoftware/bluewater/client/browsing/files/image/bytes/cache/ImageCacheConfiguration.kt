package com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.cache

import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.IDiskFileCacheConfiguration
import org.joda.time.Days
import org.joda.time.Duration

object ImageCacheConfiguration : IDiskFileCacheConfiguration {
	private const val MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024 // 100 * 1024 * 1024 for 100MB of cache

	override val cacheName = "images"
	override val maxSize = MAX_DISK_CACHE_SIZE.toLong()
	override val cacheItemLifetime: Duration by lazy { Days.days(30).toStandardDuration() }
}
