package com.lasthopesoftware.bluewater.shared.messages.application.GivenApplicationMessageRegistrations.AndTheFirstIsFaulting

import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenReceivingMessages {

	private val receivedMessages = ArrayList<TestMessage>()

	private object TestMessage : ApplicationMessage

	private val registeredApplicationMessageBus by lazy {
		ApplicationMessageBus.getApplicationMessageBus().getScopedMessageBus().apply {
			registerReceiver { _: TestMessage -> throw Exception("oh noes") }
			registerReceiver(receivedMessages::add)
		}
	}

	@BeforeAll
	fun before() {
		registeredApplicationMessageBus.use { it.sendMessage(TestMessage) }
	}

	@Test
	fun `then the message is received`() {
		assertThat(receivedMessages).containsExactly(TestMessage)
	}
}
