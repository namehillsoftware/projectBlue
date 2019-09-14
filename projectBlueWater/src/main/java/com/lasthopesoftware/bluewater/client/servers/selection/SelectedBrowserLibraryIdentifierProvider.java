package com.lasthopesoftware.bluewater.client.servers.selection;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

/**
 * Created by david on 2/12/17.
 */
public class SelectedBrowserLibraryIdentifierProvider implements ISelectedLibraryIdentifierProvider {

	private final Context context;

	public SelectedBrowserLibraryIdentifierProvider(Context context) {
		this.context = context;
	}

	@Override
	public Promise<Integer> getSelectedLibraryId() {
		return new QueuedPromise<Integer>(() -> PreferenceManager.getDefaultSharedPreferences(context).getInt(LibrarySelectionKey.chosenLibraryKey, -1), AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
