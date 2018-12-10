package com.lasthopesoftware.bluewater.client.library.views.access;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.List;

public class LibraryViewsProvider implements ProvideLibraryViews {

	private final IConnectionProvider connectionProvider;
	private final ProvideLibraryViewsUsingConnection libraryViewsUsingConnection;

	public LibraryViewsProvider(IConnectionProvider connectionProvider, ProvideLibraryViewsUsingConnection libraryViewsUsingConnection) {
		this.connectionProvider = connectionProvider;
		this.libraryViewsUsingConnection = libraryViewsUsingConnection;
	}

	@Override
	public Promise<List<Item>> promiseLibraryViews() {
		return libraryViewsUsingConnection.promiseLibraryViewsFromConnection(connectionProvider);
	}
}
