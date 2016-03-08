package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties;

import android.util.LruCache;

import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.vedsoft.lazyj.Lazy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by david on 3/7/16.
 */
class FilePropertyCache {
	public static class FilePropertiesContainer {
		public final int revision;
		private final ConcurrentHashMap<String, String> properties;

		public FilePropertiesContainer(Integer revision, Map<String, String> properties) {
			this.revision = revision;
			this.properties = new ConcurrentHashMap<>(properties);
		}

		public Map<String, String> getProperties() {
			return properties;
		}

		public void updateProperties(Map<String, String> newProperties) {
			properties.putAll(newProperties);
		}

		public void updateProperty(String key, String value) {
			properties.put(key, value);
		}
	}

	private static final int maxSize = 500;

	private final LruCache<UrlKeyHolder<Integer>, FilePropertiesContainer> propertiesCache = new LruCache<>(maxSize);

	public FilePropertiesContainer getFilePropertiesContainer(UrlKeyHolder<Integer> key) {
		synchronized (propertiesCache) {
			return propertiesCache.get(key);
		}
	}

	public void putFilePropertiesContainer(UrlKeyHolder<Integer> key, FilePropertiesContainer filePropertiesContainer) {
		synchronized (propertiesCache) {
			propertiesCache.put(key, filePropertiesContainer);
		}
	}

	private FilePropertyCache() {}

	private static final Lazy<FilePropertyCache> filePropertyCacheInstance = new Lazy<FilePropertyCache>() {

		@Override
		protected FilePropertyCache initialize() throws Exception {
			return new FilePropertyCache();
		}
	};

	public static FilePropertyCache getInstance() {
		return filePropertyCacheInstance.getObject();
	}
}
