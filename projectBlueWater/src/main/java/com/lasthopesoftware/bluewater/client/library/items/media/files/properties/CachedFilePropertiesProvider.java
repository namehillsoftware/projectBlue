package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import java.util.Map;

/**
 * Created by david on 3/7/16.
 */
public class CachedFilePropertiesProvider implements IFilePropertiesProvider {

	private final IConnectionProvider connectionProvider;
	private final IFilePropertiesProvider filePropertiesProvider;
	private final IFilePropertiesContainerRepository filePropertiesContainerRepository;

	public CachedFilePropertiesProvider(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerRepository, IFilePropertiesProvider filePropertiesProvider, int fileKey) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesContainerRepository = filePropertiesContainerRepository;
		this.filePropertiesProvider = filePropertiesProvider;
	}

	@Override
	public IPromise<Map<String, String>> promiseFileProperties(int fileKey) {
		final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), fileKey);

		final FilePropertiesContainer filePropertiesContainer = filePropertiesContainerRepository.getFilePropertiesContainer(urlKeyHolder);

		return filePropertiesContainer != null
			? new Promise<>(filePropertiesContainer.getProperties())
			: this.filePropertiesProvider.promiseFileProperties(fileKey);
	}
}
