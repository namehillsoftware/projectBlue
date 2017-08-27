package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.messenger.promises.Promise;

/**
 * Created by david on 2/12/17.
 */

public class SelectedBrowserLibraryProvider implements ISelectedBrowserLibraryProvider {

	private final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider;
	private final ILibraryProvider libraryProvider;

	public SelectedBrowserLibraryProvider(ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider, ILibraryProvider libraryProvider) {
		this.selectedLibraryIdentifierProvider = selectedLibraryIdentifierProvider;
		this.libraryProvider = libraryProvider;
	}

	@Override
	public Promise<Library> getBrowserLibrary() {
		return libraryProvider.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId());
	}
}
