package com.lasthopesoftware.bluewater.shared.messages

interface SendTypedMessages<ScopedMessage : TypedMessage> {
	fun <Message : ScopedMessage> sendMessage(message: Message)
}
