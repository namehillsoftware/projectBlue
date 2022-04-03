package com.lasthopesoftware.bluewater.shared.messages.application

import android.content.Context
import android.os.Handler

class ApplicationMessageBus private constructor(
	private val context: Context,
	private val registrations: HaveApplicationMessageRegistrations
) :
	RegisterForApplicationMessages by registrations,
	SendApplicationMessages
{
	companion object {
		fun Context.getApplicationMessageBus() =
			ApplicationMessageBus(this, ApplicationMessageRegistrations)
	}

	private val handler by lazy { Handler(context.mainLooper) }

	override fun <T : ApplicationMessage> sendMessage(message: T) {
		fun broadcastToReceivers() {
			registrations.getRegistrations(message.javaClass).forEach { it(message) }
		}

		if (Thread.currentThread() == handler.looper.thread) broadcastToReceivers()
		else handler.post(::broadcastToReceivers)
	}
}
