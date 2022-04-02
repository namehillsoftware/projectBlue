package com.lasthopesoftware.bluewater.shared.messages.application

interface HaveApplicationMessageRegistrations : RegisterForApplicationMessages {
	fun <Message : ApplicationMessage> getRegistrations(messageClass: Class<Message>): Collection<(Message) -> Unit>
}
