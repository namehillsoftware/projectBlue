package com.lasthopesoftware.bluewater.client.browsing.library.events.GivenALibraryChangedBroadcast

import android.os.Looper.getMainLooper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class WhenReceivingTheBroadcast {

	companion object {
		private val activityController by lazy {
			val activity = Robolectric.buildActivity(BrowserEntryActivity::class.java).create()
			activity.get().getApplicationMessageBus()
				.sendMessage(BrowserLibrarySelection.LibraryChosenMessage(LibraryId(4)))
			shadowOf(getMainLooper()).idle()
			activity
		}
	}

	@Test
	fun thenTheActivityIsFinished() {
		assertThat(activityController.get().isFinishing).isTrue
	}
}
