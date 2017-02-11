package com.lasthopesoftware.bluewater.client.library.repository;

import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 2/11/17.
 */
public interface ILibraryProvider {
	IPromise<Library> getLibrary(int libraryId);

	IPromise<Library> saveLibrary(Library library);
}
