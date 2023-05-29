package com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration

import org.joda.time.Duration

interface IDiskFileCacheConfiguration {
    val cacheName: String
    val maxSize: Long
    val cacheItemLifetime: Duration?
}
