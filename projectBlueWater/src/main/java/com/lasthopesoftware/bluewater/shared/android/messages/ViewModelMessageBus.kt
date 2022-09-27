package com.lasthopesoftware.bluewater.shared.android.messages

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.TypedMessage
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus

class ViewModelMessageBus<ScopedMessage : TypedMessage> :
	RegisterForTypedMessages<ScopedMessage>,
	SendTypedMessages<ScopedMessage>,
	ViewModel()
{
	private val typedMessageBus = lazy { TypedMessageBus<ScopedMessage>() }

	override fun <T : ScopedMessage> sendMessage(message: T) = typedMessageBus.value.sendMessage(message)

	override fun <Message : ScopedMessage> registerForClass(messageClass: Class<Message>, receiver: (Message) -> Unit): AutoCloseable =
		typedMessageBus.value.registerForClass(messageClass, receiver)

	override fun <Message : ScopedMessage> unregisterReceiver(receiver: (Message) -> Unit) =
		typedMessageBus.value.unregisterReceiver(receiver)

	override fun onCleared() {
		if (typedMessageBus.isInitialized())
			typedMessageBus.value.close()
	}
}

