package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.stream.supplier;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.access.ICachedFilesProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.configuration.IDiskFileCacheConfiguration;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.disk.IDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.persistence.IDiskFileCachePersistence;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.stream.CacheOutputStream;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.stream.CachedFileOutputStream;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public class DiskFileCacheStreamSupplier implements ICacheStreamSupplier {

	private final IDiskFileCacheConfiguration diskFileCacheConfiguration;
	private final IDiskFileCachePersistence diskFileCachePersistence;
	private final ICachedFilesProvider cachedFilesProvider;
	private final IDiskCacheDirectoryProvider diskCacheDirectory;

	public DiskFileCacheStreamSupplier(IDiskCacheDirectoryProvider diskCacheDirectory, IDiskFileCacheConfiguration diskFileCacheConfiguration, IDiskFileCachePersistence diskFileCachePersistence, ICachedFilesProvider cachedFilesProvider) {
		this.diskCacheDirectory = diskCacheDirectory;
		this.diskFileCacheConfiguration = diskFileCacheConfiguration;
		this.diskFileCachePersistence = diskFileCachePersistence;
		this.cachedFilesProvider = cachedFilesProvider;
	}

	public Promise<CacheOutputStream> promiseCachedFileOutputStream(final String uniqueKey) {
		return cachedFilesProvider
			.promiseCachedFile(uniqueKey)
			.then(cachedFile -> {
				final File file = cachedFile != null ? new File(cachedFile.getFileName()) : generateCacheFile(uniqueKey);

				return new CachedFileOutputStream(uniqueKey, file, diskFileCachePersistence);
			});
	}

	private File generateCacheFile(String uniqueKey) {
		final String suffix = ".cache";
		final String uniqueKeyHashCode = String.valueOf(uniqueKey.hashCode());
		final File diskCacheDir = diskCacheDirectory.getDiskCacheDirectory(diskFileCacheConfiguration);
		File file = new File(diskCacheDir, uniqueKeyHashCode + suffix);

		if (file.exists()) {
			int collisionNumber = 0;
			do {
				file = new File(diskCacheDir, uniqueKeyHashCode + "-" + collisionNumber++ + suffix);
			} while (file.exists());
		}

		return file;
	}
}
