package com.lasthopesoftware.bluewater.client.library.selection;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 2/19/17.
 */
public class BrowserLibrarySelection implements IBrowserLibrarySelection {

	private static final String chosenLibraryInt = "chosen_library";

	private final Context context;
	private final ILibraryProvider libraryProvider;

	public BrowserLibrarySelection(Context context, ILibraryProvider libraryProvider) {
		this.context = context;
		this.libraryProvider = libraryProvider;
	}

	@Override
	public IPromise<Library> selectBrowserLibrary(int libraryId) {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (libraryId != sharedPreferences.getInt(chosenLibraryInt, -1))
			sharedPreferences.edit().putInt(chosenLibraryInt, libraryId).apply();

		return libraryProvider.getLibrary(libraryId);
	}
}
