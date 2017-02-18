package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 2/12/17.
 */

public class SpecificLibraryProvider implements ISpecificLibraryProvider {

	private final int libraryId;
	private final ILibraryProvider libraryRepository;

	public SpecificLibraryProvider(int libraryId, ILibraryProvider libraryRepository) {
		this.libraryId = libraryId;
		this.libraryRepository = libraryRepository;
	}

	@Override
	public IPromise<Library> getLibrary() {
		return libraryRepository.getLibrary(libraryId);
	}
}
