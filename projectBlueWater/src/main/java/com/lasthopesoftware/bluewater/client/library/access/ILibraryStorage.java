package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.Promise;

/**
 * Created by david on 2/11/17.
 */
public interface ILibraryStorage {
	Promise<Library> saveLibrary(Library library);
}
