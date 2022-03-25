package com.lasthopesoftware.bluewater.shared.messages.application

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus

class ApplicationMessageBus private constructor(
	private val context: Context
) :
	RegisterForApplicationMessages,
	SendApplicationMessages
{
	companion object {
		@SuppressLint("StaticFieldLeak")
		private lateinit var messageBus: ApplicationMessageBus

		@Synchronized
		fun Context.getApplicationMessageBus() : ApplicationMessageBus {
			if (Companion::messageBus.isInitialized) return messageBus

			messageBus = ApplicationMessageBus(applicationContext)
			return messageBus
		}
	}

	private val typedMessageBus by lazy { TypedMessageBus<ApplicationMessage>(Handler(context.mainLooper)) }

	override fun <T : ApplicationMessage> sendMessage(message: T) = typedMessageBus.sendMessage(message)

	override fun <Message : ApplicationMessage> registerForClass(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable =
		typedMessageBus.registerForClass(messageClass, receiver)

	override fun <Message : ApplicationMessage> unregisterReceiver(receiver: (Message) -> Unit) =
		typedMessageBus.unregisterReceiver(receiver)
}
