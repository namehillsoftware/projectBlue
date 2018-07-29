package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;

/**
 * Created by david on 3/14/17.
 */

public interface IFilePropertiesContainerRepository {
	FilePropertiesContainer getFilePropertiesContainer(UrlKeyHolder<ServiceFile> key);

	void putFilePropertiesContainer(UrlKeyHolder<ServiceFile> key, FilePropertiesContainer filePropertiesContainer);
}
