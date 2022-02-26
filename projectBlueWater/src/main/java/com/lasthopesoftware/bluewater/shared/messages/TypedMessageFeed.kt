package com.lasthopesoftware.bluewater.shared.messages

import kotlinx.coroutines.flow.SharedFlow

interface TypedMessageFeed<ScopedMessage : TypedMessage> {
	val messages: SharedFlow<ScopedMessage>
}
