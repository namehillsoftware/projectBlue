package com.lasthopesoftware.bluewater.client.browsing.files.cached.disk;

import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.IDiskFileCacheConfiguration;

import java.io.File;

public interface IDiskCacheDirectoryProvider {
	File getDiskCacheDirectory(IDiskFileCacheConfiguration diskFileCacheConfiguration);
}
