package com.lasthopesoftware.bluewater.shared.messages

import java.util.concurrent.ConcurrentHashMap

class ScopedMessageRegistration<ScopedMessage : TypedMessage>(
	private val registerForTypedMessages: RegisterForTypedMessages<ScopedMessage>,
) :
	RegisterForTypedMessages<ScopedMessage>,
	AutoCloseable
{
	private val receivers = ConcurrentHashMap<Class<*>, ConcurrentHashMap<*, AutoCloseable>>()

	@Suppress("UNCHECKED_CAST")
	override fun <Message : ScopedMessage> registerReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable {
		registerForTypedMessages.registerReceiver(messageClass, receiver)
		val receiverSet = receivers.getOrPut(messageClass) { ConcurrentHashMap<(Message) -> Unit, AutoCloseable>() } as ConcurrentHashMap<(Message) -> Unit, AutoCloseable>
		val closeable = ReceiverCloseable(messageClass, receiver)
		receiverSet[receiver] = closeable
		return closeable
	}

	override fun <Message : ScopedMessage> unregisterReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit) {
		receivers[messageClass]?.remove(receiver)
		registerForTypedMessages.unregisterReceiver(messageClass, receiver)
	}

	override fun close() {
		receivers.values.flatMap { it.values }.forEach { it.close() }
	}

	private inner class ReceiverCloseable<Message : ScopedMessage>(
		private val messageClass: Class<Message>,
		private val receiver: (Message) -> Unit
	) : AutoCloseable
	{
		override fun close() {
			unregisterReceiver(messageClass, receiver)
		}
	}
}

