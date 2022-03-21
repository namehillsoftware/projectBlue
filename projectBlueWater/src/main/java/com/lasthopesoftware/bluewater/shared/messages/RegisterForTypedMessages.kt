package com.lasthopesoftware.bluewater.shared.messages

interface RegisterForTypedMessages<ScopedMessage : TypedMessage> {
	fun <Message: ScopedMessage> registerReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable
	fun <Message: ScopedMessage> unregisterReceiver(receiver: (Message) -> Unit)
}
