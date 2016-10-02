package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.vedsoft.fluent.IFluentTask;
import com.vedsoft.fluent.FluentSpecifiedTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by david on 3/7/16.
 */
public class CachedFilePropertiesProvider extends FluentSpecifiedTask<Integer, Void, Map<String, String>> {
	private static final Executor cachedFilePropertyExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);

	private final IConnectionProvider connectionProvider;
	private final int fileKey;
	private FilePropertiesProvider filePropertiesProvider;

	public CachedFilePropertiesProvider(IConnectionProvider connectionProvider, int fileKey) {
		super(cachedFilePropertyExecutor);

		this.connectionProvider = connectionProvider;
		this.fileKey = fileKey;
	}

	@Override
	protected Map<String, String> executeInBackground(Integer[] params) {
		if (isCancelled()) return new HashMap<>();

		final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), fileKey);

		final FilePropertyCache.FilePropertiesContainer filePropertiesContainer = FilePropertyCache.getInstance().getFilePropertiesContainer(urlKeyHolder);
		if (filePropertiesContainer != null)
			return filePropertiesContainer.getProperties();

		try {
			if (isCancelled()) return new HashMap<>();

			filePropertiesProvider = new FilePropertiesProvider(connectionProvider, fileKey);
			return filePropertiesProvider.get();
		} catch (ExecutionException | InterruptedException e) {
			setException(e);
		}

		return new HashMap<>();
	}

	@Override
	public IFluentTask<Integer,Void,Map<String,String>> cancel(boolean interrupt) {
		super.cancel(interrupt);

		if (filePropertiesProvider != null)
			filePropertiesProvider.cancel(interrupt);

		return this;
	}
}
