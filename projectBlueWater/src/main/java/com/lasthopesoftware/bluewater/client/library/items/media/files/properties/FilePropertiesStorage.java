package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import org.slf4j.LoggerFactory;

public class FilePropertiesStorage {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FilePropertiesStorage.class);
	private final IConnectionProvider connectionProvider;
	private final IFilePropertiesContainerRepository filePropertiesContainerRepository;

	public static void storeFileProperty(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerRepository, ServiceFile serviceFile, String property, String value, boolean isFormatted) {
		new FilePropertiesStorage(connectionProvider, filePropertiesContainerRepository).promiseFileUpdate(serviceFile, property, value, isFormatted);
	}

	public FilePropertiesStorage(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerRepository) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesContainerRepository = filePropertiesContainerRepository;
	}

	public Promise<Void> promiseFileUpdate(ServiceFile serviceFile, String property, String value, boolean isFormatted) {
		final Promise<Void> promisedUpdate = connectionProvider
			.call("File/SetInfo",
				"File=" + String.valueOf(serviceFile.getKey()),
				"Field=" + property,
				"Value=" + value,
				"formatted=" + (isFormatted ? "1" : "0"))
			.then(new VoidResponse<>(response -> {
				final int responseCode = response.code();
				logger.info("api/v1/File/SetInfo responded with a response code of " + responseCode);
			}));

		promisedUpdate.excuse(new VoidResponse<>(ioe -> logger.error("There was an error opening the connection", ioe)));

		promisedUpdate.eventually(v -> RevisionChecker.promiseRevision(connectionProvider))
			.then(revision -> {
				final UrlKeyHolder<ServiceFile> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile);
				final FilePropertiesContainer filePropertiesContainer = filePropertiesContainerRepository.getFilePropertiesContainer(urlKeyHolder);

				if (filePropertiesContainer.revision == revision) filePropertiesContainer.updateProperty(property, value);

				return null;
			})
			.excuse(e -> {
				logger.warn(serviceFile.getKey() + "'s property cache item " + property + " was not updated with the new value of " + value, e);
				return null;
			});

		return promisedUpdate;
	}
}
