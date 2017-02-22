package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 2/21/17.
 */
public interface ISelectedBrowserLibraryProvider {
	IPromise<Library> getBrowserLibrary();
}
