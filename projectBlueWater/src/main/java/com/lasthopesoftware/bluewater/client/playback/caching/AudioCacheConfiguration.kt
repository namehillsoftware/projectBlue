package com.lasthopesoftware.bluewater.client.playback.caching

import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.IDiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import org.joda.time.Duration

class AudioCacheConfiguration(override val library: Library) : IDiskFileCacheConfiguration {
	override val cacheName: String = musicCacheName

	override val maxSize: Long = maxFileCacheSize

	override val cacheItemLifetime: Duration? = null

    companion object {
        private const val musicCacheName = "music"
        private const val maxFileCacheSize = 1024L * 1024L * 1024L // ~1GB
    }
}
