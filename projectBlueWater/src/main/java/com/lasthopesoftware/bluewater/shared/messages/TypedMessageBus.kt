package com.lasthopesoftware.bluewater.shared.messages

import com.lasthopesoftware.bluewater.shared.lazyLogger
import java.util.concurrent.ConcurrentHashMap

class TypedMessageBus<ScopedMessage : TypedMessage> :
	RegisterForTypedMessages<ScopedMessage>,
	SendTypedMessages<ScopedMessage>,
	AutoCloseable
{
	private val logger by lazyLogger<TypedMessageBus<ScopedMessage>>()

	private val registrationSync = Any()
	private val receivers = ConcurrentHashMap<Class<*>, ConcurrentHashMap<*, Unit>>()
	private val classesByReceiver = HashMap<(ScopedMessage) -> Unit, ConcurrentHashMap<Class<*>, Unit>>()

	@Suppress("UNCHECKED_CAST")
	override fun <T : ScopedMessage> sendMessage(message: T) {
		val typedReceivers = receivers[message.javaClass] as? ConcurrentHashMap<(T) -> Unit, Unit> ?: return
		typedReceivers.forEach { (r, _) ->
			try {
				r(message)
			} catch (e: Exception) {
				logger.error("An error occurred handling message $message with receiver $r.", e)
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <Message : ScopedMessage> registerForClass(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable {
		synchronized(registrationSync) {
			val receiverSet =
				receivers.getOrPut(messageClass) { ConcurrentHashMap<(Message) -> Unit, Unit>() } as ConcurrentHashMap<(Message) -> Unit, Unit>
			receiverSet[receiver] = Unit

			val classesSet = classesByReceiver.getOrPut(receiver as (ScopedMessage) -> Unit) { ConcurrentHashMap() }
			classesSet[messageClass] = Unit

			return ReceiverCloseable(receiver)
		}
	}

	override fun <Message : ScopedMessage> unregisterReceiver(receiver: (Message) -> Unit) {
		synchronized(registrationSync) {
			val classesSet = classesByReceiver.remove(receiver) ?: return
			for (c in classesSet) receivers[c.key]?.apply {
				remove(receiver)
				if (isEmpty())
					receivers.remove(c.key)
			}
		}
	}

	override fun close() {
		synchronized(registrationSync) {
			receivers.clear()
			classesByReceiver.clear()
		}
	}

	private inner class ReceiverCloseable<Message : ScopedMessage>(private val receiver: (Message) -> Unit)
		: AutoCloseable
	{
		override fun close() {
			unregisterReceiver(receiver)
		}
	}
}

