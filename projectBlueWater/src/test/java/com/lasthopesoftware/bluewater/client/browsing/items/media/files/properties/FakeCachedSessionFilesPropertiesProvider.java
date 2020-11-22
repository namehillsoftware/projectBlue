package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class FakeCachedSessionFilesPropertiesProvider extends CachedSessionFilePropertiesProvider {
	private final Map<ServiceFile, Map<String, String>> cachedFileProperties = new HashMap<>();

	public FakeCachedSessionFilesPropertiesProvider() {
		super(null, null, null);
	}

	@NotNull
	@Override
	public Promise<Map<String, String>> promiseFileProperties(ServiceFile serviceFile) {
		try {
			return new Promise<>(cachedFileProperties.get(serviceFile));
		} catch (Throwable e) {
			return new Promise<>(e);
		}
	}

	public void addFilePropertiesToCache(ServiceFile serviceFile, Map<String, String> fileProperties) {
		cachedFileProperties.put(serviceFile, fileProperties);
	}
}
