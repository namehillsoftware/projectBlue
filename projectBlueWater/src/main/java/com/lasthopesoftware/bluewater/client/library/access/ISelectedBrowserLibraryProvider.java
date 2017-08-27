package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.messenger.promises.Promise;

/**
 * Created by david on 2/21/17.
 */
public interface ISelectedBrowserLibraryProvider {
	Promise<Library> getBrowserLibrary();
}
