package com.lasthopesoftware.bluewater.client.servers.list.listeners;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.lasthopesoftware.bluewater.client.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;

/**
 * Created by david on 1/24/16.
 */
public class SelectServerOnClickListener implements View.OnClickListener {
	private final Library library;

	public SelectServerOnClickListener(Library library) {
		this.library = library;
	}

	@Override
	public void onClick(View v) {
		final Context context = v.getContext();
		LibrarySession.changeActiveLibrary(context, library.getId());

		final Intent browseLibraryIntent = new Intent(context, BrowseLibraryActivity.class);
		browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(browseLibraryIntent);
	}
}
