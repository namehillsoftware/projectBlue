package com.lasthopesoftware.bluewater.client.library.access.views;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.List;

public interface ProvideLibraryViews {
	Promise<List<Item>> promiseLibraryViewsFromConnection(IConnectionProvider connectionProvider);
}
