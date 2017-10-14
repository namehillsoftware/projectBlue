package com.lasthopesoftware.bluewater.client.library.items.media.audio;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.configuration.IDiskFileCacheConfiguration;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

import org.joda.time.Days;


public class AudioCacheConfiguration implements IDiskFileCacheConfiguration {
	private final Library library;
	private static final String musicCacheName = "music";
	private static final long maxFileCacheSize = 5368709000L; // 5GB

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
	public Days getCacheExpirationDays() {
		return null;
	}
}
