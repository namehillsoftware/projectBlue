package com.lasthopesoftware.resources

import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.TypedMessage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import java.util.concurrent.ConcurrentHashMap

class RecordingTypedMessageBus<ScopedMessage : TypedMessage> :
	RegisterForTypedMessages<ScopedMessage>,
	SendTypedMessages<ScopedMessage>
{
	private val receivers = ConcurrentHashMap<Class<*>, ConcurrentHashMap<*, Unit>>()
	private val classesByReceiver = ConcurrentHashMap<(ScopedMessage) -> Unit, ConcurrentHashMap<Class<*>, Unit>>()

	val recordedMessages = ArrayList<ScopedMessage>()

	override fun <T : ScopedMessage> sendMessage(message: T) {
		@Suppress("UNCHECKED_CAST")
		fun broadcastToReceivers() {
			val typedReceivers = receivers[message.javaClass] as? ConcurrentHashMap<(T) -> Unit, Unit> ?: return
			typedReceivers.forEach { (r, _) -> r(message) }
		}

		recordedMessages.add(message)
		broadcastToReceivers()
	}

	@Suppress("UNCHECKED_CAST")
	override fun <Message : ScopedMessage> registerForClass(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable {
		val receiverSet = receivers.getOrPut(messageClass) { ConcurrentHashMap<(Message) -> Unit, Unit>() } as ConcurrentHashMap<(Message) -> Unit, Unit>
		receiverSet[receiver] = Unit

		val classesSet = classesByReceiver.getOrPut(receiver as ((ScopedMessage) -> Unit)?) { ConcurrentHashMap() }
		classesSet[messageClass] = Unit

		return ReceiverCloseable(receiver)
	}

	override fun <Message : ScopedMessage> unregisterReceiver(receiver: (Message) -> Unit) {
		val classesSet = classesByReceiver.remove(receiver) ?: return
		for (c in classesSet)
			receivers[c.key]?.remove(receiver)
	}

	private inner class ReceiverCloseable<Message : ApplicationMessage>(private val receiver: (Message) -> Unit)
		: AutoCloseable
	{
		override fun close() {
			unregisterReceiver(receiver)
		}
	}
}
