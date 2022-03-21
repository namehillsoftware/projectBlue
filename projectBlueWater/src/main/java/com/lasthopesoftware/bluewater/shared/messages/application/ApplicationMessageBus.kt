package com.lasthopesoftware.bluewater.shared.messages.application

import android.content.Context
import android.os.Handler
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus

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
			if (Companion::messageBus.isInitialized) return messageBus

			messageBus = ApplicationMessageBus(applicationContext)
			return messageBus
		}
	}

	private val typedMessageBus = lazy { TypedMessageBus<ApplicationMessage>(Handler(context.mainLooper)) }

	override fun <T : ApplicationMessage> sendMessage(message: T) = typedMessageBus.value.sendMessage(message)

	override fun <Message : ApplicationMessage> registerReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable =
		typedMessageBus.value.registerReceiver(messageClass, receiver)

	override fun <Message : ApplicationMessage> unregisterReceiver(receiver: (Message) -> Unit) =
		typedMessageBus.value.unregisterReceiver(receiver)

	override fun close() {
		if (typedMessageBus.isInitialized()) typedMessageBus.value.close()
	}
}
