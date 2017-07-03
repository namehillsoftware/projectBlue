package com.lasthopesoftware.bluewater.client.servers.selection;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.messenger.promise.Promise;

/**
 * Created by david on 2/19/17.
 */

public interface IBrowserLibrarySelection {
	Promise<Library> selectBrowserLibrary(int libraryId);
}
