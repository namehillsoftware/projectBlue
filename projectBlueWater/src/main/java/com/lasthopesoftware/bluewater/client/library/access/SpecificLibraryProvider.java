package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.messenger.promises.Promise;

/**
 * Created by david on 2/12/17.
 */

public class SpecificLibraryProvider implements ISpecificLibraryProvider {

	private final int libraryId;
	private final ILibraryProvider libraryProvider;

	public SpecificLibraryProvider(int libraryId, ILibraryProvider libraryProvider) {
		this.libraryId = libraryId;
		this.libraryProvider = libraryProvider;
	}

	@Override
	public Promise<Library> getLibrary() {
		return libraryProvider.getLibrary(libraryId);
	}
}
