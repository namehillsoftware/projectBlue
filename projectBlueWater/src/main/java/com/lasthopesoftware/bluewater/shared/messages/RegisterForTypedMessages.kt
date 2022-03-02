package com.lasthopesoftware.bluewater.shared.messages

interface RegisterForTypedMessages<ScopedMessage : TypedMessage> {
	fun <Message: ScopedMessage> registerReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit)
	fun <Message: ScopedMessage> unregisterReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit)
}
