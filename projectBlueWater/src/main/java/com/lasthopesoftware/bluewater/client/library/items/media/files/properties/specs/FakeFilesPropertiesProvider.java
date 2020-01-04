package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.specs;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.ProvideLibraryFileProperties;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class FakeFilesPropertiesProvider implements ProvideLibraryFileProperties {
	private final Map<ServiceFile, Map<String, String>> cachedFileProperties = new HashMap<>();

	@NotNull
	@Override
	public Promise<Map<String, String>> promiseFileProperties(LibraryId libraryId, ServiceFile serviceFile) {
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
