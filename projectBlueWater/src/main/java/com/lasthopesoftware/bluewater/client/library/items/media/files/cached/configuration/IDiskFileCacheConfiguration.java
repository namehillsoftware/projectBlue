package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.configuration;

import com.lasthopesoftware.bluewater.client.library.repository.Library;

import org.joda.time.Duration;

public interface IDiskFileCacheConfiguration {
	String getCacheName();
	Library getLibrary();
	long getMaxSize();
	Duration getCacheItemLifetime();
}
