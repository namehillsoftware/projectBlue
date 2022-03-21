package com.lasthopesoftware.bluewater.shared.messages

class ScopedApplicationMessageBus(
	registerForApplicationMessages: RegisterForApplicationMessages,
	sendApplicationMessages: SendApplicationMessages,
) :
	SendApplicationMessages,
	RegisterForApplicationMessages,
	AutoCloseable
{
	private val scopedMessageBus = ScopedMessageBus(registerForApplicationMessages, sendApplicationMessages)

	override fun <Message : ApplicationMessage> registerReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable {
		return scopedMessageBus.registerReceiver(messageClass, receiver)
	}

	override fun <Message : ApplicationMessage> unregisterReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit) {
		scopedMessageBus.unregisterReceiver(messageClass, receiver)
	}

	override fun <Message : ApplicationMessage> sendMessage(message: Message) {
		scopedMessageBus.sendMessage(message)
	}

	override fun close() {
		scopedMessageBus.close()
	}
}
