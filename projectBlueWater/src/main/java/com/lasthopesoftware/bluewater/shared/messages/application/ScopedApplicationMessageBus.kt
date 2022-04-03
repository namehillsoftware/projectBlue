package com.lasthopesoftware.bluewater.shared.messages.application

import com.lasthopesoftware.bluewater.shared.messages.ScopedMessageBus

fun ApplicationMessageBus.getScopedMessageBus() = ScopedApplicationMessageBus(this, this)

class ScopedApplicationMessageBus(
	registerForApplicationMessages: RegisterForApplicationMessages,
	sendApplicationMessages: SendApplicationMessages,
) :
	SendApplicationMessages,
	RegisterForApplicationMessages,
	AutoCloseable
{
	private val scopedMessageBus = ScopedMessageBus(registerForApplicationMessages, sendApplicationMessages)

	override fun <Message : ApplicationMessage> registerForClass(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable =
		scopedMessageBus.registerForClass(messageClass, receiver)

	override fun <Message : ApplicationMessage> unregisterReceiver(receiver: (Message) -> Unit) =
		scopedMessageBus.unregisterReceiver(receiver)

	override fun <Message : ApplicationMessage> sendMessage(message: Message) = scopedMessageBus.sendMessage(message)

	override fun close() = scopedMessageBus.close()
}
