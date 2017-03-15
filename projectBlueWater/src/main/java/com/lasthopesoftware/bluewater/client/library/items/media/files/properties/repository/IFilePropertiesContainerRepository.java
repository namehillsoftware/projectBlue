package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository;

import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;

/**
 * Created by david on 3/14/17.
 */

public interface IFilePropertiesContainerRepository {
	FilePropertiesContainer getFilePropertiesContainer(UrlKeyHolder<Integer> key);

	void putFilePropertiesContainer(UrlKeyHolder<Integer> key, FilePropertiesContainer filePropertiesContainer);
}
