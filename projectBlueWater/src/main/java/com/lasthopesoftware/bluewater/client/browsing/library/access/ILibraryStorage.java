package com.lasthopesoftware.bluewater.client.browsing.library.access;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

/**
 * Created by david on 2/11/17.
 */
public interface ILibraryStorage {
	Promise<Library> saveLibrary(Library library);
}
