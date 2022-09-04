package com.lasthopesoftware.bluewater.shared.messages.GivenTypedMessageRegistrations.AndTheFirstIsFaulting

import android.content.Context
import android.os.Handler
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.shared.messages.ScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.TypedMessage
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Test

private val receivedMessages = ArrayList<TestMessage>()

private object TestMessage : TypedMessage

private val typedMessageBus by lazy {
	val typedMessageBus = TypedMessageBus<TestMessage>(Handler(ApplicationProvider.getApplicationContext<Context>().mainLooper))
	ScopedMessageBus(typedMessageBus, typedMessageBus).apply {
		registerReceiver { throw Exception("sore") }
		registerReceiver(receivedMessages::add)
	}
}

class WhenReceivingMessages : AndroidContext() {
	companion object {
		@JvmStatic
		@AfterClass
		fun cleanup() {
			typedMessageBus.close()
		}
	}

	override fun before() {
		typedMessageBus.sendMessage(TestMessage)
	}

	@Test
	fun `then the message is received`() {
		assertThat(receivedMessages).containsExactly(TestMessage)
	}
}
