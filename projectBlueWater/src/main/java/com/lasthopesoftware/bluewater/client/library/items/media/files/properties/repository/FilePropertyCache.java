package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository;

import android.util.LruCache;

import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.vedsoft.lazyj.Lazy;

/**
 * Created by david on 3/7/16.
 */
public class FilePropertyCache implements IFilePropertiesContainerRepository {

	private static final int maxSize = 500;

	private final LruCache<UrlKeyHolder<Integer>, FilePropertiesContainer> propertiesCache = new LruCache<>(maxSize);

	@Override
	public FilePropertiesContainer getFilePropertiesContainer(UrlKeyHolder<Integer> key) {
		synchronized (propertiesCache) {
			return propertiesCache.get(key);
		}
	}

	@Override
	public void putFilePropertiesContainer(UrlKeyHolder<Integer> key, FilePropertiesContainer filePropertiesContainer) {
		synchronized (propertiesCache) {
			propertiesCache.put(key, filePropertiesContainer);
		}
	}

	private FilePropertyCache() {}

	private static final Lazy<FilePropertyCache> filePropertyCacheInstance = new Lazy<>(FilePropertyCache::new);

	public static FilePropertyCache getInstance() {
		return filePropertyCacheInstance.getObject();
	}
}
