package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 2/12/17.
 */

public class SelectedBrowserLibraryProvider implements ISelectedBrowserLibraryProvider {

	private final ILibraryProvider libraryProvider;
	private final int selectedLibraryId;

	public SelectedBrowserLibraryProvider(ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider, ILibraryProvider libraryProvider) {
		this.selectedLibraryId = selectedLibraryIdentifierProvider.getSelectedLibraryId();
		this.libraryProvider = libraryProvider;
	}

	@Override
	public IPromise<Library> getBrowserLibrary() {
		return libraryProvider.getLibrary(selectedLibraryId);
	}
}
