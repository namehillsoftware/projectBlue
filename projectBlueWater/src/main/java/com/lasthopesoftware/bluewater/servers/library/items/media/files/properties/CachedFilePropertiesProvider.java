package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.servers.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.vedsoft.fluent.FluentTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by david on 3/7/16.
 */
public class CachedFilePropertiesProvider extends FluentTask<Integer, Void, Map<String, String>> {
	private static final Executor cachedFilePropertyExecutor = Executors.newCachedThreadPool();

	private IConnectionProvider connectionProvider;
	private int fileKey;

	public CachedFilePropertiesProvider(IConnectionProvider connectionProvider, int fileKey) {
		super(cachedFilePropertyExecutor);

		this.connectionProvider = connectionProvider;
		this.fileKey = fileKey;
	}

	@Override
	protected Map<String, String> executeInBackground(Integer[] params) {
		final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), fileKey);

		final FilePropertyCache.FilePropertiesContainer filePropertiesContainer = FilePropertyCache.getInstance().getFilePropertiesContainer(urlKeyHolder);
		if (filePropertiesContainer != null)
			return filePropertiesContainer.getProperties();

		try {
			return new FilePropertiesProvider(connectionProvider, fileKey).get();
		} catch (ExecutionException | InterruptedException e) {
			setException(e);
		}

		return new HashMap<>();
	}
}
