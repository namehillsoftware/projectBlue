package com.lasthopesoftware.bluewater.client.browsing.library.views.access;

import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface ProvideLibraryViewsUsingConnection {
	Promise<Collection<ViewItem>> promiseLibraryViewsFromConnection(IConnectionProvider connectionProvider);
}
