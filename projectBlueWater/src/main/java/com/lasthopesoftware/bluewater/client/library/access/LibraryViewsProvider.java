package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemResponse;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellableMessageTask;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;
import com.lasthopesoftware.providers.AbstractProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

public class LibraryViewsProvider implements CancellableMessageTask<List<Item>> {

    private static final Logger logger = LoggerFactory.getLogger(LibraryViewsProvider.class);

    public final static String browseLibraryParameter = "Browse/Children";

    private static List<Item> cachedFileSystemItems;
    private static Integer revision;

    private final ConnectionProvider connectionProvider;
	private final Integer serverRevision;

	public static Promise<List<Item>> provide(ConnectionProvider connectionProvider) {
		return
			RevisionChecker.promiseRevision(connectionProvider)
				.eventually(serverRevision -> {
					synchronized(browseLibraryParameter) {
						if (cachedFileSystemItems != null && revision.equals(serverRevision))
							return new Promise<>(cachedFileSystemItems);
					}

					return new QueuedPromise<>(new LibraryViewsProvider(connectionProvider, serverRevision), AbstractProvider.providerExecutor);
				});
    }

    private LibraryViewsProvider(ConnectionProvider connectionProvider, Integer serverRevision) {
        this.connectionProvider = connectionProvider;
		this.serverRevision = serverRevision;
	}

	@Override
	public List<Item> prepareMessage(CancellationToken cancellationToken) throws Throwable {
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
	}
}
