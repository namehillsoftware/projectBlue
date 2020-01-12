package com.lasthopesoftware.bluewater.client.browsing.library.access;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

/**
 * Created by david on 2/12/17.
 */

public class SpecificLibraryProvider implements ISpecificLibraryProvider {

	private final LibraryId libraryId;
	private final ILibraryProvider libraryProvider;

	public SpecificLibraryProvider(LibraryId libraryId, ILibraryProvider libraryProvider) {
		this.libraryId = libraryId;
		this.libraryProvider = libraryProvider;
	}

	@Override
	public Promise<Library> getLibrary() {
		return libraryProvider.getLibrary(libraryId);
	}
}
