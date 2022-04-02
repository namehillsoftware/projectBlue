package com.lasthopesoftware.bluewater.client.browsing.library.events.GivenARandomBroadcast

import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
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
		ApplicationMessageBus.getInstance().sendMessage(object : ApplicationMessage{})
	}

	@Test
	fun thenTheActivityIsNotFinished() {
		assertThat(activityController.value.get().isFinishing).isFalse
	}
}
