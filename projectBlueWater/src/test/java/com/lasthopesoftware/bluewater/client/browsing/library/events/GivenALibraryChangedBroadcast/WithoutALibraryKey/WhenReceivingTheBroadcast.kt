package com.lasthopesoftware.bluewater.client.browsing.library.events.GivenALibraryChangedBroadcast.WithoutALibraryKey

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.robolectric.Robolectric

class WhenReceivingTheBroadcast : AndroidContext() {

	companion object {
		private val activityController = Robolectric.buildActivity(BrowserEntryActivity::class.java).create()
	}

    override fun before() {
        val localBroadcastManager = LocalBroadcastManager.getInstance(activityController.get())
        val broadcastIntent = Intent(BrowserLibrarySelection.libraryChosenEvent)
        localBroadcastManager.sendBroadcast(broadcastIntent)
    }

    @Test
    fun thenTheActivityIsNotFinished() {
        assertThat(activityController.get().isFinishing).isFalse
    }
}
