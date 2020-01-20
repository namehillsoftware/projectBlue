package com.lasthopesoftware.bluewater.client.browsing.library.items.media.audio;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.configuration.IDiskFileCacheConfiguration;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

import org.joda.time.Duration;


public class AudioCacheConfiguration implements IDiskFileCacheConfiguration {
	private final Library library;
	private static final String musicCacheName = "music";
	private static final long maxFileCacheSize = 2L * 1024L * 1024L * 1024L; // ~2GB

	public AudioCacheConfiguration(Library library) {
		this.library = library;
	}

	@Override
	public String getCacheName() {
		return musicCacheName;
	}

	@Override
	public Library getLibrary() {
		return library;
	}

	@Override
	public long getMaxSize() {
		return maxFileCacheSize;
	}

	@Override
	public Duration getCacheItemLifetime() {
		return null;
	}
}
