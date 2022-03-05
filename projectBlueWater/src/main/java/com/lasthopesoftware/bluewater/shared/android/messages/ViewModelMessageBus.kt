package com.lasthopesoftware.bluewater.shared.android.messages

import android.os.Handler
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.TypedMessage
import java.util.concurrent.ConcurrentHashMap

class ViewModelMessageBus<ScopedMessage : TypedMessage>(
	private val handler: Handler
) :
	RegisterForTypedMessages<ScopedMessage>,
	SendTypedMessages<ScopedMessage>,
	ViewModel()
{
	private val receivers = ConcurrentHashMap<Class<*>, ConcurrentHashMap<*, Unit>>()

	override fun <T : ScopedMessage> sendMessage(message: T) {
		@Suppress("UNCHECKED_CAST")
		fun broadcastToReceivers() {
			val typedReceivers = receivers[message.javaClass] as? ConcurrentHashMap<(T) -> Unit, Unit> ?: return
			typedReceivers.forEach { (r, _) -> r(message) }
		}

		if (Thread.currentThread() == handler.looper.thread) broadcastToReceivers()
		else handler.post(::broadcastToReceivers)
	}

	@Suppress("UNCHECKED_CAST")
	override fun <Message : ScopedMessage> registerReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit) {
		val receiverSet = receivers.getOrPut(messageClass) { ConcurrentHashMap<(Message) -> Unit, Unit>() } as ConcurrentHashMap<(Message) -> Unit, Unit>
		receiverSet[receiver] = Unit
	}

	override fun <Message : ScopedMessage> unregisterReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit) {
		receivers[messageClass]?.remove(receiver)
	}

	override fun onCleared() {
		receivers.clear()
	}
}

