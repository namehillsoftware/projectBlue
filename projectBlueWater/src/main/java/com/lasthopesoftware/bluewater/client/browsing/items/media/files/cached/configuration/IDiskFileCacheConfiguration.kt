package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.configuration;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

import org.joda.time.Duration;

public interface IDiskFileCacheConfiguration {
	String getCacheName();
	Library getLibrary();
	long getMaxSize();
	Duration getCacheItemLifetime();
}
