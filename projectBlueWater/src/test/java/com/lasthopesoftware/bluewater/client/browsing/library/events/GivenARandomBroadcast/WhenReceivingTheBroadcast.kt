package com.lasthopesoftware.bluewater.client.browsing.library.events.GivenARandomBroadcast

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.robolectric.Robolectric

class WhenReceivingTheBroadcast : AndroidContext() {

	companion object {
		private val activityController = lazy {
			Robolectric.buildActivity(BrowserEntryActivity::class.java).create().start().resume()
		}
	}

	override fun before() {
		System.setProperty("javax.net.ssl.trustStoreType", "JKS")
		val localBroadcastManager = LocalBroadcastManager.getInstance(activityController.value.get())
		val broadcastIntent = Intent("absr4")
		localBroadcastManager.sendBroadcast(broadcastIntent)
	}

	@Test
	fun thenTheActivityIsNotFinished() {
		assertThat(activityController.value.get().isFinishing).isFalse
	}
}
