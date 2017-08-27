package com.lasthopesoftware.bluewater.client.servers.selection;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.messenger.promises.Promise;

/**
 * Created by david on 2/19/17.
 */
public class BrowserLibrarySelection implements IBrowserLibrarySelection {

	public static final String libraryChosenEvent = MagicPropertyBuilder.buildMagicPropertyName(BrowserLibrarySelection.class, "libraryChosenEvent");

	private final Context context;
	private final LocalBroadcastManager localBroadcastManager;
	private final ILibraryProvider libraryProvider;

	public BrowserLibrarySelection(Context context, LocalBroadcastManager localBroadcastManager, ILibraryProvider libraryProvider) {
		this.context = context;
		this.localBroadcastManager = localBroadcastManager;
		this.libraryProvider = libraryProvider;
	}

	@Override
	public Promise<Library> selectBrowserLibrary(int libraryId) {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (libraryId == sharedPreferences.getInt(LibrarySelectionKey.chosenLibraryKey, -1)) return libraryProvider.getLibrary(libraryId);

		sharedPreferences.edit().putInt(LibrarySelectionKey.chosenLibraryKey, libraryId).apply();

		final Intent broadcastIntent = new Intent(libraryChosenEvent);
		broadcastIntent.putExtra(LibrarySelectionKey.chosenLibraryKey, libraryId);
		localBroadcastManager.sendBroadcast(broadcastIntent);

		return libraryProvider.getLibrary(libraryId);
	}
}
