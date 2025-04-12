package com.lasthopesoftware.bluewater.shared.messages.GivenTypedMessageRegistrations.AndTheFirstIsFaulting

import com.lasthopesoftware.bluewater.shared.messages.ScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.TypedMessage
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenReceivingMessages {

	private val receivedMessages = ArrayList<TestMessage>()

	private object TestMessage : TypedMessage

	private val typedMessageBus by lazy {
		val typedMessageBus = TypedMessageBus<TestMessage>()
		ScopedMessageBus(typedMessageBus, typedMessageBus).apply {
			registerReceiver { throw Exception("sore") }
			registerReceiver(receivedMessages::add)
		}
	}

	@BeforeAll
	fun before() {
		typedMessageBus.use { it.sendMessage(TestMessage) }
	}

	@Test
	fun `then the message is received`() {
		assertThat(receivedMessages).containsExactly(TestMessage)
	}
}
