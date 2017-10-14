package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.configuration;

import com.lasthopesoftware.bluewater.client.library.repository.Library;

public interface IDiskFileCacheConfiguration {
	String getCacheName();
	Library getLibrary();
	long getMaxSize();
}
