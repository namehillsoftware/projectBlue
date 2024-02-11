package com.lasthopesoftware.bluewater.shared.messages.application

class GuaranteedDeliveryApplicationMessageBus<T>(private val inner: T) : SendApplicationMessages, RegisterForApplicationMessages by inner, AutoCloseable
	where T : SendApplicationMessages, T: RegisterForApplicationMessages, T : AutoCloseable
{

	private val messageLock = Any()

	override fun <Message : ApplicationMessage> sendMessage(message: Message) {
		synchronized(messageLock) {
			inner.sendMessage(message)
		}
	}

	override fun close() {
		synchronized(messageLock) {
			inner.close()
		}
	}
}
