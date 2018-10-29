package com.lasthopesoftware.bluewater.client.library.events.specs.GivenALibraryChangedBroadcast;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.client.servers.selection.BrowserLibrarySelection;
import com.lasthopesoftware.bluewater.client.servers.selection.LibrarySelectionKey;
import com.lasthopesoftware.specs.AndroidContext;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenReceivingTheBroadcast extends AndroidContext {

	private static final ActivityController<BrowseLibraryActivity> activityController = Robolectric.buildActivity(BrowseLibraryActivity.class).create();

	@Override
	public void before() {
		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(activityController.get());

		final Intent broadcastIntent = new Intent(BrowserLibrarySelection.libraryChosenEvent);
		broadcastIntent.putExtra(LibrarySelectionKey.chosenLibraryKey, 4);
		localBroadcastManager.sendBroadcast(broadcastIntent);
	}

	@Test
	public void thenTheActivityIsFinished() {
		assertThat(activityController.get().isFinishing()).isTrue();
	}
}
