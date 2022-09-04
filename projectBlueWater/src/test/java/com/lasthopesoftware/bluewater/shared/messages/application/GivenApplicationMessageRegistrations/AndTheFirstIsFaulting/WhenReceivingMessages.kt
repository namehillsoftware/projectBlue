package com.lasthopesoftware.bluewater.shared.messages.application.GivenApplicationMessageRegistrations.AndTheFirstIsFaulting

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Test

private val receivedMessages = ArrayList<TestMessage>()

private object TestMessage : ApplicationMessage

private val registeredApplicationMessageBus by lazy {
	ApplicationProvider.getApplicationContext<Context>().getApplicationMessageBus().getScopedMessageBus().apply {
		registerReceiver { _: TestMessage -> throw Exception("oh noes") }
		registerReceiver(receivedMessages::add)
	}
}

class WhenReceivingMessages : AndroidContext() {
	companion object {
		@JvmStatic
		@AfterClass
		fun cleanup() {
			registeredApplicationMessageBus.close()
		}
	}

	override fun before() {
		registeredApplicationMessageBus.sendMessage(TestMessage)
	}

	@Test
	fun `then the message is received`() {
		assertThat(receivedMessages).containsExactly(TestMessage)
	}
}
