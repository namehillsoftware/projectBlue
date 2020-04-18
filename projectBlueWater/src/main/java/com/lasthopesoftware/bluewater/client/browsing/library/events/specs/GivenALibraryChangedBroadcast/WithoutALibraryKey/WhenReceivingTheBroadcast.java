package com.lasthopesoftware.bluewater.client.browsing.library.events.specs.GivenALibraryChangedBroadcast.WithoutALibraryKey;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection;
import com.lasthopesoftware.specs.AndroidContext;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenReceivingTheBroadcast extends AndroidContext {

	private static final ActivityController<BrowserEntryActivity> activityController = Robolectric.buildActivity(BrowserEntryActivity.class).create();

	@Override
	public void before() {
		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(activityController.get());

		final Intent broadcastIntent = new Intent(BrowserLibrarySelection.libraryChosenEvent);
		localBroadcastManager.sendBroadcast(broadcastIntent);
	}

	@Test
	public void thenTheActivityIsNotFinished() {
		assertThat(activityController.get().isFinishing()).isFalse();
	}
}
