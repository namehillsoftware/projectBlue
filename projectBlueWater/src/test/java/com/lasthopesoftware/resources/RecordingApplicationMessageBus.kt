package com.lasthopesoftware.resources

import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import java.util.concurrent.ConcurrentHashMap

open class RecordingApplicationMessageBus :
	RegisterForApplicationMessages,
	SendApplicationMessages
{
	private val receivers = ConcurrentHashMap<Class<*>, ConcurrentHashMap<*, Unit>>()
	private val classesByReceiver = ConcurrentHashMap<(ApplicationMessage) -> Unit, ConcurrentHashMap<Class<*>, Unit>>()

	val recordedMessages = ArrayList<ApplicationMessage>()

	@Suppress("UNCHECKED_CAST")
	override fun <T : ApplicationMessage> sendMessage(message: T) {
		recordedMessages.add(message)
		val typedReceivers = receivers[message.javaClass] as? ConcurrentHashMap<(T) -> Unit, Unit> ?: return
		typedReceivers.forEach { (r, _) -> r(message) }
	}

	@Suppress("UNCHECKED_CAST")
	override fun <Message : ApplicationMessage> registerForClass(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable {
		val receiverSet = receivers.getOrPut(messageClass) { ConcurrentHashMap<(Message) -> Unit, Unit>() } as ConcurrentHashMap<(Message) -> Unit, Unit>
		receiverSet[receiver] = Unit

		val classesSet = classesByReceiver.getOrPut(receiver as ((ApplicationMessage) -> Unit)?) { ConcurrentHashMap() }
		classesSet[messageClass] = Unit

		return ReceiverCloseable(receiver)
	}

	override fun <Message : ApplicationMessage> unregisterReceiver(receiver: (Message) -> Unit) {
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
