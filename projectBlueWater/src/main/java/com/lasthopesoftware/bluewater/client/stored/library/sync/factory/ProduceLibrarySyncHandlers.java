package com.lasthopesoftware.bluewater.client.stored.library.sync.factory;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;

public interface ProduceLibrarySyncHandlers {
	LibrarySyncHandler getNewSyncHandler(IUrlProvider urlProvider, Library library);
}
