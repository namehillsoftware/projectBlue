package com.lasthopesoftware.bluewater.client.library.selection;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 2/19/17.
 */

public interface IBrowserLibrarySelection {
	IPromise<Library> selectBrowserLibrary(int libraryId);
}
