package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.providers.AbstractProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.MessageWriter;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

public class FilePropertiesStorage {

	private final IConnectionProvider connectionProvider;
	private final IFilePropertiesContainerRepository filePropertiesContainerRepository;

	public static void storeFileProperty(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerRepository, ServiceFile serviceFile, String property, String value, boolean isFormatted) {
		AbstractProvider.providerExecutor.execute(() -> new FilePropertiesStorageWriter(connectionProvider, filePropertiesContainerRepository, serviceFile, property, value, isFormatted).prepareMessage());
	}

	public FilePropertiesStorage(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerRepository) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesContainerRepository = filePropertiesContainerRepository;
	}

	public Promise<Void> promiseFileUpdate(ServiceFile serviceFile, String property, String value, boolean isFormatted) {
		return new QueuedPromise<>(
			new FilePropertiesStorageWriter(
				connectionProvider,
				filePropertiesContainerRepository,
				serviceFile,
				property,
				value,
				isFormatted),
			AbstractProvider.providerExecutor);
	}

	private static class FilePropertiesStorageWriter implements MessageWriter<Void> {

		private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FilePropertiesStorageWriter.class);
		private final IConnectionProvider connectionProvider;
		private final IFilePropertiesContainerRepository filePropertiesContainerRepository;
		private final ServiceFile serviceFile;
		private final String property;
		private final String value;
		private final boolean isFormatted;

		FilePropertiesStorageWriter(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerRepository, ServiceFile serviceFile, String property, String value, boolean isFormatted) {
			this.connectionProvider = connectionProvider;
			this.filePropertiesContainerRepository = filePropertiesContainerRepository;
			this.serviceFile = serviceFile;
			this.property = property;
			this.value = value;
			this.isFormatted = isFormatted;
		}

		@Override
		public Void prepareMessage() {
//		if (cancellation.isCancelled()) return null;

			try {
				final HttpURLConnection connection = connectionProvider.getConnection(
					"File/SetInfo",
					"File=" + String.valueOf(serviceFile.getKey()),
					"Field=" + property,
					"Value=" + value,
					"formatted=" + (isFormatted ? "1" : "0"));
				try {
					final int responseCode = connection.getResponseCode();
					logger.info("api/v1/File/SetInfo responded with a response code of " + responseCode);
				} finally {
					connection.disconnect();
				}
			} catch (IOException ioe) {
				logger.error("There was an error opening the connection", ioe);
			}

			RevisionChecker.promiseRevision(connectionProvider)
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

			return null;
		}
	}
}
