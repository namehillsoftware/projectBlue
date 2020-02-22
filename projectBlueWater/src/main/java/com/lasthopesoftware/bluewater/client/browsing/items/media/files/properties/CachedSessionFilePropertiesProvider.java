package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Map;

/**
 * Created by david on 3/7/16.
 */
public class CachedSessionFilePropertiesProvider implements ProvideFilePropertiesForSession {

	private final IConnectionProvider connectionProvider;
	private final ProvideFilePropertiesForSession filePropertiesProvider;
	private final IFilePropertiesContainerRepository filePropertiesContainerRepository;

	public CachedSessionFilePropertiesProvider(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerRepository, ProvideFilePropertiesForSession filePropertiesProvider) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesContainerRepository = filePropertiesContainerRepository;
		this.filePropertiesProvider = filePropertiesProvider;
	}

	@Override
	public Promise<Map<String, String>> promiseFileProperties(ServiceFile serviceFile) {
		final UrlKeyHolder<ServiceFile> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.urlProvider.getBaseUrl(), serviceFile);

		final FilePropertiesContainer filePropertiesContainer = filePropertiesContainerRepository.getFilePropertiesContainer(urlKeyHolder);

		return filePropertiesContainer != null
			? new Promise<>(filePropertiesContainer.getProperties())
			: this.filePropertiesProvider.promiseFileProperties(serviceFile);
	}
}
