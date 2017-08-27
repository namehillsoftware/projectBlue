package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.specs;

import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;

import java.util.HashMap;


public class FakeFilePropertiesContainer implements IFilePropertiesContainerRepository {

	private final HashMap<UrlKeyHolder<Integer>, FilePropertiesContainer> storage = new HashMap<>();

	@Override
	public FilePropertiesContainer getFilePropertiesContainer(UrlKeyHolder<Integer> key) {
		return storage.get(key);
	}

	@Override
	public void putFilePropertiesContainer(UrlKeyHolder<Integer> key, FilePropertiesContainer filePropertiesContainer) {
		storage.put(key, filePropertiesContainer);
	}
}
