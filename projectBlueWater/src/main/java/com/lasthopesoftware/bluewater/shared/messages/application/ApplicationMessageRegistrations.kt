package com.lasthopesoftware.bluewater.shared.messages.application

object ApplicationMessageRegistrations : HaveApplicationMessageRegistrations {
	private val registrationSync = Any()
	private val receivers = HashMap<Class<out ApplicationMessage>, HashMap<*, Unit>>()
	private val classesByReceiver = HashMap<(ApplicationMessage) -> Unit, HashMap<Class<*>, Unit>>()

	@Suppress("UNCHECKED_CAST")
	override fun <Message : ApplicationMessage> getRegistrations(messageClass: Class<Message>): Collection<(Message) -> Unit> {
		val typedReceivers = receivers[messageClass] as? HashMap<(Message) -> Unit, Unit>
		return typedReceivers?.keys ?: emptySet()
	}

	@Suppress("UNCHECKED_CAST")
	override fun <Message : ApplicationMessage> registerForClass(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable {
		synchronized(registrationSync) {
			val receiverSet =
				receivers.getOrPut(messageClass) { HashMap<(Message) -> Unit, Unit>() } as HashMap<(Message) -> Unit, Unit>
			receiverSet[receiver] = Unit

			val classesSet = classesByReceiver.getOrPut(receiver as (ApplicationMessage) -> Unit) { HashMap() }
			classesSet[messageClass] = Unit

			return ReceiverCloseable(receiver)
		}
	}

	override fun <Message : ApplicationMessage> unregisterReceiver(receiver: (Message) -> Unit) {
		synchronized(registrationSync) {
			val classesSet = classesByReceiver.remove(receiver) ?: return
			for (c in classesSet) receivers[c.key]?.apply {
				remove(receiver)
				if (isEmpty())
					receivers.remove(c.key)
			}
		}
	}

	private class ReceiverCloseable<Message : ApplicationMessage>(private val receiver: (Message) -> Unit)
		: AutoCloseable
	{
		override fun close() {
			unregisterReceiver(receiver)
		}
	}
}
