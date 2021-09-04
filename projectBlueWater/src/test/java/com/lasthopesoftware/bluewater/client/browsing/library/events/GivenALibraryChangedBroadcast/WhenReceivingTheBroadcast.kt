package com.lasthopesoftware.bluewater.client.browsing.library.events.GivenALibraryChangedBroadcast

import android.content.Intent
import android.os.Looper.getMainLooper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.settings.repository.ApplicationConstants
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class WhenReceivingTheBroadcast {

	companion object {
		private val activityController = lazy {
			val activity = Robolectric.buildActivity(BrowserEntryActivity::class.java).create()
			val localBroadcastManager = LocalBroadcastManager.getInstance(activity.get())
			val broadcastIntent = Intent(BrowserLibrarySelection.libraryChosenEvent)
			broadcastIntent.putExtra(ApplicationConstants.PreferenceConstants.chosenLibraryKey, 4)
			localBroadcastManager.sendBroadcast(broadcastIntent)
			shadowOf(getMainLooper()).idle()
			activity
		}
	}

	@Test
	fun thenTheActivityIsFinished() {
		assertThat(activityController.value.get().isFinishing).isTrue
	}
}
