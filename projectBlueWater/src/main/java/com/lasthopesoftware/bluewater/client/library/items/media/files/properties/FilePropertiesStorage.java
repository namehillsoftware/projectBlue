package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.lasthopesoftware.providers.AbstractProvider;
import com.vedsoft.futures.callables.CarelessFunction;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

public class FilePropertiesStorage {

	private final IConnectionProvider connectionProvider;
	private final IFilePropertiesContainerRepository filePropertiesContainerRepository;

	public static void storeFileProperty(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerRepository, int fileKey, String property, String value, boolean isFormatted) {
		AbstractProvider.providerExecutor.execute(() -> new FilePropertiesStorageTask(connectionProvider, filePropertiesContainerRepository, fileKey, property, value, isFormatted).result());
	}

	public FilePropertiesStorage(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerRepository) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesContainerRepository = filePropertiesContainerRepository;
	}

	public Promise<Void> promiseFileUpdate(ServiceFile serviceFile, String property, String value, boolean isFormatted) {
		return new QueuedPromise<>(
			new FilePropertiesStorageTask(
				connectionProvider,
				filePropertiesContainerRepository,
				serviceFile.getKey(),
				property,
				value,
				isFormatted),
			AbstractProvider.providerExecutor);
	}

	private static class FilePropertiesStorageTask implements CarelessFunction<Void> {

		private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FilePropertiesStorageTask.class);
		private final IConnectionProvider connectionProvider;
		private final IFilePropertiesContainerRepository filePropertiesContainerRepository;
		private final int fileKey;
		private final String property;
		private final String value;
		private final boolean isFormatted;

		FilePropertiesStorageTask(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerRepository, int fileKey, String property, String value, boolean isFormatted) {
			this.connectionProvider = connectionProvider;
			this.filePropertiesContainerRepository = filePropertiesContainerRepository;
			this.fileKey = fileKey;
			this.property = property;
			this.value = value;
			this.isFormatted = isFormatted;
		}

		@Override
		public Void result() {
//		if (cancellation.isCancelled()) return null;

			try {
				final HttpURLConnection connection = connectionProvider.getConnection(
					"File/SetInfo",
					"File=" + String.valueOf(fileKey),
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
					final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), fileKey);
					final FilePropertiesContainer filePropertiesContainer = filePropertiesContainerRepository.getFilePropertiesContainer(urlKeyHolder);

					if (filePropertiesContainer.revision == revision) filePropertiesContainer.updateProperty(property, value);

					return null;
				})
				.excuse(e -> {
					logger.warn(fileKey + "'s property cache item " + property + " was not updated with the new value of " + value, e);
					return null;
				});

			return null;
		}
	}
}
