package com.lasthopesoftware.bluewater.client.library.items.media.image;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.configuration.IDiskFileCacheConfiguration;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

import org.joda.time.Days;


public final class ImageCacheConfiguration implements IDiskFileCacheConfiguration {

	private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 100 * 1024 * 1024 for 100MB of cache
	private static final Days MAX_DAYS_IN_CACHE = Days.days(30);
	private static final String IMAGES_CACHE_NAME = "images";
	private final Library library;

	public ImageCacheConfiguration(Library library) {

		this.library = library;
	}

	@Override
	public String getCacheName() {
		return IMAGES_CACHE_NAME;
	}

	@Override
	public Library getLibrary() {
		return library;
	}

	@Override
	public long getMaxSize() {
		return MAX_DISK_CACHE_SIZE;
	}

	@Override
	public Days getCacheExpirationDays() {
		return MAX_DAYS_IN_CACHE;
	}
}
