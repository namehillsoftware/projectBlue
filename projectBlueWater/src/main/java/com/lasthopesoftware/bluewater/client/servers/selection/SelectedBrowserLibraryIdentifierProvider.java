package com.lasthopesoftware.bluewater.client.servers.selection;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by david on 2/12/17.
 */
public class SelectedBrowserLibraryIdentifierProvider implements ISelectedLibraryIdentifierProvider {

	private final Context context;

	public SelectedBrowserLibraryIdentifierProvider(Context context) {
		this.context = context;
	}

	@Override
	public int getSelectedLibraryId() {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(LibrarySelectionKey.chosenLibraryKey, -1);
	}
}
