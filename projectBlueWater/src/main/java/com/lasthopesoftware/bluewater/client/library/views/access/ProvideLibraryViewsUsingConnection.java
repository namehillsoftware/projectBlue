package com.lasthopesoftware.bluewater.client.library.views.access;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.List;

public interface ProvideLibraryViewsUsingConnection {
	Promise<List<Item>> promiseLibraryViewsFromConnection(IConnectionProvider connectionProvider);
}
