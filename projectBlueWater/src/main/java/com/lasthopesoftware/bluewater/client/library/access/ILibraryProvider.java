package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

/**
 * Created by david on 2/18/17.
 */
public interface ILibraryProvider {
	Promise<Library> getLibrary(int libraryId);
	Promise<Library> getLibrary(LibraryId libraryId);
	Promise<Collection<Library>> getAllLibraries();
}
