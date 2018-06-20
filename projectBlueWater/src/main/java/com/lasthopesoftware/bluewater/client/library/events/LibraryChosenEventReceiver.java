package com.lasthopesoftware.bluewater.client.library.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.client.servers.selection.LibrarySelectionKey;

public class LibraryChosenEventReceiver extends BroadcastReceiver {

	private final BrowseLibraryActivity browseLibraryActivity;

	public LibraryChosenEventReceiver(BrowseLibraryActivity browseLibraryActivity) {
		this.browseLibraryActivity = browseLibraryActivity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final int chosenLibrary = intent.getIntExtra(LibrarySelectionKey.chosenLibraryKey, -1);
		if (chosenLibrary >= 0)
			browseLibraryActivity.finishAffinity();
	}
}
