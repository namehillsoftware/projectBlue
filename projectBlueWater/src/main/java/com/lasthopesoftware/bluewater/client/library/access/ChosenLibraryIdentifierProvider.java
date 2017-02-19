package com.lasthopesoftware.bluewater.client.library.access;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by david on 2/12/17.
 */
public class ChosenLibraryIdentifierProvider implements IChosenLibraryIdentifierProvider {

	private static final String chosenLibraryInt = "chosen_library";

	private final Context context;

	public ChosenLibraryIdentifierProvider(Context context) {
		this.context = context;
	}

	@Override
	public int getChosenLibraryId() {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(chosenLibraryInt, -1);
	}
}
