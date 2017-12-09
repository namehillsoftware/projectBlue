package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.configuration.IDiskFileCacheConfiguration;

import java.io.File;

public interface IDiskCacheDirectoryProvider {
	File getDiskCacheDirectory(IDiskFileCacheConfiguration diskFileCacheConfiguration);
}
