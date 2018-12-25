package com.lasthopesoftware.bluewater.client.library.views.access;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemResponse;
import com.namehillsoftware.handoff.promises.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LibraryViewsByConnectionProvider implements ProvideLibraryViewsUsingConnection {

    private static final Logger logger = LoggerFactory.getLogger(LibraryViewsByConnectionProvider.class);

    public final static String browseLibraryParameter = "Browse/Children";

    private static List<Item> cachedFileSystemItems;
    private static Integer revision;

	public static Promise<List<Item>> provide(IConnectionProvider connectionProvider) {
		return new LibraryViewsByConnectionProvider().promiseLibraryViewsFromConnection(connectionProvider);
    }

	@Override
	public Promise<List<Item>> promiseLibraryViewsFromConnection(IConnectionProvider connectionProvider) {
		return RevisionChecker.promiseRevision(connectionProvider)
				.eventually(serverRevision -> {
					synchronized(browseLibraryParameter) {
						if (cachedFileSystemItems != null && revision.equals(serverRevision))
							return new Promise<>(cachedFileSystemItems);
					}

					return connectionProvider.promiseResponse(browseLibraryParameter)
						.then(response -> {
							final List<Item> items = ItemResponse.GetItems(response.body().byteStream());

							synchronized (browseLibraryParameter) {
								revision = serverRevision;
								cachedFileSystemItems = items;
							}

							return items;
						});
				});
	}
}
