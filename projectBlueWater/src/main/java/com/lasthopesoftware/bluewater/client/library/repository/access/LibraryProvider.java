package com.lasthopesoftware.bluewater.client.library.repository.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 2/12/17.
 */

public class LibraryProvider implements ILibraryProvider {

	private final int libraryId;
	private final ILibraryRepository libraryRepository;

	public LibraryProvider(int libraryId, ILibraryRepository libraryRepository) {
		this.libraryId = libraryId;
		this.libraryRepository = libraryRepository;
	}

	@Override
	public IPromise<Library> getLibrary() {
		return libraryRepository.getLibrary(libraryId);
	}
}
