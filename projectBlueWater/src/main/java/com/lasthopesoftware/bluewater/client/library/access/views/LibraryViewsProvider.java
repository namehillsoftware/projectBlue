package com.lasthopesoftware.bluewater.client.library.access.views;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemResponse;
import com.lasthopesoftware.providers.AbstractProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

public class LibraryViewsProvider implements ProvideLibraryViews {

    private static final Logger logger = LoggerFactory.getLogger(LibraryViewsProvider.class);

    public final static String browseLibraryParameter = "Browse/Children";

    private static List<Item> cachedFileSystemItems;
    private static Integer revision;

	public static Promise<List<Item>> provide(IConnectionProvider connectionProvider) {
		return new LibraryViewsProvider().promiseLibraryViewsFromConnection(connectionProvider);
    }

	@Override
	public Promise<List<Item>> promiseLibraryViewsFromConnection(IConnectionProvider connectionProvider) {
		return RevisionChecker.promiseRevision(connectionProvider)
				.eventually(serverRevision -> {
					synchronized(browseLibraryParameter) {
						if (cachedFileSystemItems != null && revision.equals(serverRevision))
							return new Promise<>(cachedFileSystemItems);
					}

					return new QueuedPromise<>((cancellationToken) -> {
						if (cancellationToken.isCancelled()) return null;

						try {
							final HttpURLConnection connection = connectionProvider.getConnection(browseLibraryParameter);
							try {
								try (final InputStream is = connection.getInputStream()) {
									final List<Item> items = ItemResponse.GetItems(is);

									synchronized (browseLibraryParameter) {
										revision = serverRevision;
										cachedFileSystemItems = items;
									}

									return items;
								} catch (IOException e) {
									logger.error("There was an error getting the inputstream", e);
									throw e;
								}
							} finally {
								connection.disconnect();
							}
						} catch (IOException ioe) {
							logger.error("There was an error opening the connection", ioe);
						}

						return null;
					}, AbstractProvider.providerExecutor);
				});
	}
}
