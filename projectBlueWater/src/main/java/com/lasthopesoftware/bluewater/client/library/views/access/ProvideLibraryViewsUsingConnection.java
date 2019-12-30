package com.lasthopesoftware.bluewater.client.library.views.access;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.views.ViewItem;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface ProvideLibraryViewsUsingConnection {
	Promise<Collection<ViewItem>> promiseLibraryViewsFromConnection(IConnectionProvider connectionProvider);
}
