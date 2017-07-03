package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.messenger.promise.Promise;

import java.util.Collection;

/**
 * Created by david on 2/18/17.
 */
public interface ILibraryProvider {
	Promise<Library> getLibrary(int libraryId);
	Promise<Collection<Library>> getAllLibraries();
}
