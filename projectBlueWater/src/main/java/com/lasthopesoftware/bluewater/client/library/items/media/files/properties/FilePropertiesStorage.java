package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.providers.AbstractInputStreamProvider;
import com.lasthopesoftware.providers.Cancellation;

import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class FilePropertiesStorage extends AbstractInputStreamProvider<Void> {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FilePropertiesStorage.class);
	private final IConnectionProvider connectionProvider;
	private final int fileKey;
	private final String property;
	private final String value;

	public static void storeFileProperty(IConnectionProvider connectionProvider, int fileKey, String property, String value) {
		new FilePropertiesStorage(connectionProvider, fileKey, property, value).promiseData();
	}

	private FilePropertiesStorage(IConnectionProvider connectionProvider, int fileKey, String property, String value) {
		super(
			connectionProvider,
			"File/SetInfo",
			"File=" + String.valueOf(fileKey),
			"Field=" + property,
			"Value=" + value,
			"formatted=0");

		this.connectionProvider = connectionProvider;
		this.fileKey = fileKey;
		this.property = property;
		this.value = value;
	}

	@Override
	protected Void getData(InputStream inputStream, Cancellation cancellation) {
		final RevisionChecker revisionChecker = new RevisionChecker(connectionProvider);
		revisionChecker.execute();

		final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), fileKey);
		final FilePropertiesContainer filePropertiesContainer = FilePropertyCache.getInstance().getFilePropertiesContainer(urlKeyHolder);

		try {
			if (filePropertiesContainer.revision == revisionChecker.get()) filePropertiesContainer.updateProperty(property, value);
		} catch (ExecutionException | InterruptedException e) {
			logger.warn(this.fileKey + "'s property cache item " + this.property + " was not updated with the new value of " + this.value, e);
		}

		return null;
	}
}
