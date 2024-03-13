package com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration

import org.joda.time.Days
import org.joda.time.Duration

interface DiskFileCacheConfiguration {
    val cacheName: String
    val maxSize: Long
    val cacheItemLifetime: Duration?
}

data object ImageCacheConfiguration : DiskFileCacheConfiguration {
	private const val MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024 // 100 * 1024 * 1024 for 100MB of cache

	override val cacheName = "images"
	override val maxSize = MAX_DISK_CACHE_SIZE.toLong()
	override val cacheItemLifetime: Duration by lazy { Days.days(30).toStandardDuration() }
}

data object AudioCacheConfiguration : DiskFileCacheConfiguration {
	private const val MUSIC_CACHE_NAME = "music"
	private const val MAX_FILE_CACHE_SIZE = 1024L * 1024L * 1024L // ~1GB

	override val cacheName: String = MUSIC_CACHE_NAME

	override val maxSize: Long = MAX_FILE_CACHE_SIZE

	override val cacheItemLifetime: Duration? = null
}
