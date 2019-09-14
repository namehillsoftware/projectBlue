package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

public class ChosenLibraryProvider implements ISpecificLibraryProvider, PromisedResponse<Integer, Library> {

	private final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider;
	private final ILibraryProvider libraryProvider;

	public ChosenLibraryProvider(ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider, ILibraryProvider libraryProvider) {
		this.selectedLibraryIdentifierProvider = selectedLibraryIdentifierProvider;
		this.libraryProvider = libraryProvider;
	}

	@Override
	public Promise<Library> getLibrary() {
		return selectedLibraryIdentifierProvider.getSelectedLibraryId().eventually(this);
	}

	@Override
	public Promise<Library> promiseResponse(Integer libraryId) {
		return libraryProvider.getLibrary(libraryId);
	}
}
