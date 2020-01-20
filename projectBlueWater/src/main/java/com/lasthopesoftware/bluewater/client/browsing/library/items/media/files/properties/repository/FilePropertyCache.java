package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.properties.repository;

import android.util.LruCache;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.namehillsoftware.lazyj.Lazy;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FilePropertyCache implements IFilePropertiesContainerRepository {

	private static final int maxSize = 500;

	private final LruCache<UrlKeyHolder<ServiceFile>, FilePropertiesContainer> propertiesCache = new LruCache<>(maxSize);
	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	@Override
	public FilePropertiesContainer getFilePropertiesContainer(@NotNull UrlKeyHolder<ServiceFile> key) {
		final Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			return propertiesCache.get(key);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void putFilePropertiesContainer(@NotNull UrlKeyHolder<ServiceFile> key, @NotNull FilePropertiesContainer filePropertiesContainer) {
		final Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			propertiesCache.put(key, filePropertiesContainer);
		} finally {
			writeLock.unlock();
		}
	}

	private FilePropertyCache() {}

	private static final Lazy<FilePropertyCache> filePropertyCacheInstance = new Lazy<>(FilePropertyCache::new);

	public static FilePropertyCache getInstance() {
		return filePropertyCacheInstance.getObject();
	}
}
