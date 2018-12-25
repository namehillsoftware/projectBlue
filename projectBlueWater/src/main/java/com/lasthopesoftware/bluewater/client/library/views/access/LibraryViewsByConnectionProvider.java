package com.lasthopesoftware.bluewater.client.library.views.access;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemResponse;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.InputStream;
import java.util.List;

public class LibraryViewsByConnectionProvider implements ProvideLibraryViewsUsingConnection {

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
							try (final InputStream is = response.body().byteStream()) {
								final List<Item> items = ItemResponse.GetItems(is);

								synchronized (browseLibraryParameter) {
									revision = serverRevision;
									cachedFileSystemItems = items;
								}

								return items;
							}
						});
				});
	}
}
