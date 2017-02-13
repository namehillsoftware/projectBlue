package com.lasthopesoftware.bluewater.client.library.repository.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 2/11/17.
 */
public interface ILibraryRepository {
	IPromise<Library> getLibrary(int libraryId);

	IPromise<Library> saveLibrary(Library library);
}
