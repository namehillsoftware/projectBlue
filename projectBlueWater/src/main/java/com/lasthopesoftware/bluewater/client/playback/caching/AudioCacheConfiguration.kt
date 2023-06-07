package com.lasthopesoftware.bluewater.client.playback.caching

import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.IDiskFileCacheConfiguration
import org.joda.time.Duration

object AudioCacheConfiguration : IDiskFileCacheConfiguration {
	private const val musicCacheName = "music"
	private const val maxFileCacheSize = 1024L * 1024L * 1024L // ~1GB

	override val cacheName: String = musicCacheName

	override val maxSize: Long = maxFileCacheSize

	override val cacheItemLifetime: Duration? = null
}
