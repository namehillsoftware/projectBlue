package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.specs;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;

import java.util.HashMap;


public class FakeFilePropertiesContainer implements IFilePropertiesContainerRepository {

	private final HashMap<UrlKeyHolder<ServiceFile>, FilePropertiesContainer> storage = new HashMap<>();

	@Override
	public FilePropertiesContainer getFilePropertiesContainer(UrlKeyHolder<ServiceFile> key) {
		return storage.get(key);
	}

	@Override
	public void putFilePropertiesContainer(UrlKeyHolder<ServiceFile> key, FilePropertiesContainer filePropertiesContainer) {
		storage.put(key, filePropertiesContainer);
	}
}
