package com.lasthopesoftware.bluewater.client.browsing.library.access.session;

import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;

/**
 * Created by david on 2/12/17.
 */
public class SelectedBrowserLibraryIdentifierProvider implements ISelectedLibraryIdentifierProvider {

	private final Context context;

	public SelectedBrowserLibraryIdentifierProvider(Context context) {
		this.context = context;
	}

	@Override
	public LibraryId getSelectedLibraryId() {
		final int libraryId = PreferenceManager.getDefaultSharedPreferences(context).getInt(LibrarySelectionKey.chosenLibraryKey, -1);
		return libraryId > -1 ? new LibraryId(libraryId) : null;
	}
}
