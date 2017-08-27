package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FilePropertiesContainer {
	public final int revision;
	private final ConcurrentHashMap<String, String> properties;

	public FilePropertiesContainer(Integer revision, Map<String, String> properties) {
		this.revision = revision;
		this.properties = new ConcurrentHashMap<>(properties);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void updateProperty(String key, String value) {
		properties.put(key, value);
	}
}
