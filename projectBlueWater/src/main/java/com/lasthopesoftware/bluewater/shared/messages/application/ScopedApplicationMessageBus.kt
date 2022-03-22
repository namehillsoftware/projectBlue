package com.lasthopesoftware.bluewater.shared.messages.application

import android.content.Context
import com.lasthopesoftware.bluewater.shared.messages.ScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus

fun Context.scopedApplicationMessageBus() : ScopedApplicationMessageBus {
	val applicationMessageBus = getApplicationMessageBus()
	return ScopedApplicationMessageBus(applicationMessageBus, applicationMessageBus)
}

class ScopedApplicationMessageBus(
	registerForApplicationMessages: RegisterForApplicationMessages,
	sendApplicationMessages: SendApplicationMessages,
) :
	SendApplicationMessages,
	RegisterForApplicationMessages,
	AutoCloseable
{
	private val scopedMessageBus = ScopedMessageBus(registerForApplicationMessages, sendApplicationMessages)

	override fun <Message : ApplicationMessage> registerReceiver(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable =
		scopedMessageBus.registerReceiver(messageClass, receiver)

	override fun <Message : ApplicationMessage> unregisterReceiver(receiver: (Message) -> Unit) =
		scopedMessageBus.unregisterReceiver(receiver)

	override fun <Message : ApplicationMessage> sendMessage(message: Message) = scopedMessageBus.sendMessage(message)

	override fun close() = scopedMessageBus.close()
}
