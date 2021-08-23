package com.lasthopesoftware.bluewater.client.browsing.library.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity;
import com.lasthopesoftware.bluewater.settings.repository.ApplicationConstants;

public class LibraryChosenEventReceiver extends BroadcastReceiver {

	private final BrowserEntryActivity browserEntryActivity;

	public LibraryChosenEventReceiver(BrowserEntryActivity browserEntryActivity) {
		this.browserEntryActivity = browserEntryActivity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final int chosenLibrary = intent.getIntExtra(ApplicationConstants.PreferenceConstants.chosenLibraryKey, -1);
		if (chosenLibrary >= 0)
			browserEntryActivity.finishAffinity();
	}
}
