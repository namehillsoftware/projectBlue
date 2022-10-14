package com.lasthopesoftware.bluewater.shared.messages

import java.util.concurrent.ConcurrentHashMap

class ScopedMessageBus<ScopedMessage : TypedMessage>(
	private val registerForTypedMessages: RegisterForTypedMessages<ScopedMessage>,
	private val sendTypedMessages: SendTypedMessages<ScopedMessage>
) :
	RegisterForTypedMessages<ScopedMessage>,
	SendTypedMessages<ScopedMessage> by sendTypedMessages,
	AutoCloseable
{
	private val receivers = ConcurrentHashMap<Any, AutoCloseable>()

	override fun <Message : ScopedMessage> registerForClass(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable {
		val innerCloseable = registerForTypedMessages.registerForClass(messageClass, receiver)
		val closeable = ReceiverCloseable(receiver, innerCloseable)
		receivers[receiver] = closeable
		return closeable
	}

	override fun <Message : ScopedMessage> unregisterReceiver(receiver: (Message) -> Unit) {
		receivers.remove(receiver)
		registerForTypedMessages.unregisterReceiver(receiver)
	}

	override fun close() {
		receivers.forEach { it.value.close() }
	}

	private inner class ReceiverCloseable<Message : ScopedMessage>(
		private val receiver: (Message) -> Unit,
		private val innerCloseable: AutoCloseable
	)
		: AutoCloseable
	{
		override fun close() {
			unregisterReceiver(receiver)
			innerCloseable.close()
		}
	}
}

