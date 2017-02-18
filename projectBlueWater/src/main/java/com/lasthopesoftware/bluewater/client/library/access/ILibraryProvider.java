package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;

import java.util.Collection;

/**
 * Created by david on 2/18/17.
 */
public interface ILibraryProvider {
	IPromise<Library> getLibrary(int libraryId);
	IPromise<Collection<Library>> getAllLibraries();
}
