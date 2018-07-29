package com.lasthopesoftware.bluewater.client.servers.list.listeners;

import android.view.View;

import com.lasthopesoftware.bluewater.client.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.IBrowserLibrarySelection;

/**
 * Created by david on 1/24/16.
 */
public class SelectServerOnClickListener implements View.OnClickListener {
	private final Library library;
	private final IBrowserLibrarySelection browserLibrarySelection;

	public SelectServerOnClickListener(Library library, IBrowserLibrarySelection browserLibrarySelection) {
		this.library = library;
		this.browserLibrarySelection = browserLibrarySelection;
	}

	@Override
	public void onClick(View v) {
		browserLibrarySelection.selectBrowserLibrary(library.getId());

		InstantiateSessionConnectionActivity.startNewConnection(v.getContext());
	}
}
