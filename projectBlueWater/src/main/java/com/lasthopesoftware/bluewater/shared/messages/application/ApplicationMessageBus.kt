package com.lasthopesoftware.bluewater.shared.messages.application

import android.content.Context
import android.os.Handler
import com.lasthopesoftware.bluewater.shared.lazyLogger

class ApplicationMessageBus private constructor(
	private val context: Context,
	private val registrations: HaveApplicationMessageRegistrations
) :
	RegisterForApplicationMessages by registrations,
	SendApplicationMessages
{
	companion object {
		private val logger by lazyLogger<ApplicationMessageBus>()

		fun Context.getApplicationMessageBus() =
			ApplicationMessageBus(this, ApplicationMessageRegistrations)
	}

	private val handler by lazy { Handler(context.mainLooper) }

	override fun <T : ApplicationMessage> sendMessage(message: T) {
		fun broadcastToReceivers() {
			registrations.getRegistrations(message.javaClass).forEach {
				try {
					it(message)
				} catch (e: Exception) {
					logger.error("An error occurred handling message $message with receiver $it.", e)
				}
			}
		}

		if (Thread.currentThread() == handler.looper.thread) broadcastToReceivers()
		else handler.post(::broadcastToReceivers)
	}
}
