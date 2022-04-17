package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.configuration

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import org.joda.time.Duration

interface IDiskFileCacheConfiguration {
    val cacheName: String
    val library: Library
    val maxSize: Long
    val cacheItemLifetime: Duration?
}
