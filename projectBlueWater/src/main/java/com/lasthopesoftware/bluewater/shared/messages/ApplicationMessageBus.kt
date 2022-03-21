package com.lasthopesoftware.bluewater.shared.messages

import android.content.Context
import android.os.Handler
import java.util.concurrent.ConcurrentHashMap

class ApplicationMessageBus private constructor(
	private val context: Context
) :
	RegisterForApplicationMessages,
	SendApplicationMessages,
	AutoCloseable
{
	companion object {
		private lateinit var messageBus: ApplicationMessageBus

		fun Context.getApplicationMessageBus() : ApplicationMessageBus {
			if (::messageBus.isInitialized) return messageBus

			messageBus = ApplicationMessageBus(applicationContext)
			return messageBus
		}

		inline fun <reified M : ApplicationMessage> ApplicationMessageBus.registerReceiver(noinline receiver: (M) -> Unit): AutoCloseable =
			registerReceiver(M::class.java, receiver)
	}

	private val handler by lazy { Handler(context.mainLooper) }

	private val receivers = ConcurrentHashMap<Class<*>, ConcurrentHashMap<*, Unit>>()

	override fun <T : ApplicationMessage> sendMessage(message: T) {
		@Suppress("UNCHECKED_CAST")
		fun broadcastToReceivers() {
			val typedReceivers = receivers[message.javaClass] as? ConcurrentHashMap<(T) -> Unit, Unit> ?: return
			typedReceivers.forEach { (r, _) -> r(message) }
		}

		if (Thread.currentThread() == handler.looper.thread) broadcastToReceivers()
		else handler.post(::broadcastToReceivers)
	}

	@Suppress("UNCHECKED_CAST")
	override fun <Message : ApplicationMessage> registerReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable {
		val receiverSet = receivers.getOrPut(messageClass) { ConcurrentHashMap<(Message) -> Unit, Unit>() } as ConcurrentHashMap<(Message) -> Unit, Unit>
		receiverSet[receiver] = Unit
		return ReceiverCloseable(messageClass, receiver)
	}

	override fun <Message : ApplicationMessage> unregisterReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit) {
		receivers[messageClass]?.remove(receiver)
	}

	override fun close() {
		receivers.clear()
	}

	private inner class ReceiverCloseable<Message : ApplicationMessage>(
		private val messageClass: Class<Message>,
		private val receiver: (Message) -> Unit)
		: AutoCloseable
	{
		override fun close() {
			unregisterReceiver(messageClass, receiver)
		}
	}
}
