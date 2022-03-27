package com.lasthopesoftware.bluewater.shared.messages.application

import android.annotation.SuppressLint
import android.app.Application
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
		private lateinit var instance: ApplicationMessageBus

		@Synchronized fun initializeInstance(application: Application): ApplicationMessageBus {
			if (!::instance.isInitialized)
				instance = ApplicationMessageBus(application)

			return instance
		}

		fun getInstance() =
			if (::instance.isInitialized) instance
			else throw IllegalStateException("Instance should be initialized in application root")
	}

	private val typedMessageBus by lazy { TypedMessageBus<ApplicationMessage>(Handler(context.mainLooper)) }

	override fun <T : ApplicationMessage> sendMessage(message: T) = typedMessageBus.sendMessage(message)

	override fun <Message : ApplicationMessage> registerForClass(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable =
		typedMessageBus.registerForClass(messageClass, receiver)

	override fun <Message : ApplicationMessage> unregisterReceiver(receiver: (Message) -> Unit) =
		typedMessageBus.unregisterReceiver(receiver)
}
