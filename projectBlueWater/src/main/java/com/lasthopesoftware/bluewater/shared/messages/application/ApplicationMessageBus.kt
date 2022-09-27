package com.lasthopesoftware.bluewater.shared.messages.application

import com.lasthopesoftware.bluewater.shared.lazyLogger

class ApplicationMessageBus private constructor(
	private val registrations: HaveApplicationMessageRegistrations
) :
	RegisterForApplicationMessages by registrations,
	SendApplicationMessages
{
	companion object {
		private val logger by lazyLogger<ApplicationMessageBus>()

		fun getApplicationMessageBus() =
			ApplicationMessageBus(ApplicationMessageRegistrations)
	}

	override fun <T : ApplicationMessage> sendMessage(message: T) {
		registrations.getRegistrations(message.javaClass).forEach {
			try {
				it(message)
			} catch (e: Exception) {
				logger.error("An error occurred handling message $message with receiver $it.", e)
			}
		}
	}
}
