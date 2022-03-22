package com.lasthopesoftware.bluewater.shared.messages

interface RegisterForTypedMessages<ScopedMessage : TypedMessage> {
	fun <Message: ScopedMessage> registerForClass(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable
	fun <Message: ScopedMessage> unregisterReceiver(receiver: (Message) -> Unit)
}
