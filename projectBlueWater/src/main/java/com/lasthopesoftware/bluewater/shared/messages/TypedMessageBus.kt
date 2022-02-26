package com.lasthopesoftware.bluewater.shared.messages

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class TypedMessageBus<ScopedMessage : TypedMessage> : TypedMessageFeed<ScopedMessage>, SendTypedMessages<ScopedMessage> {
	private val messageFlow = MutableSharedFlow<ScopedMessage>()

	override val messages = messageFlow.asSharedFlow()

	override fun <T : ScopedMessage> sendMessage(message: T) {
		messageFlow.tryEmit(message)
	}
}

