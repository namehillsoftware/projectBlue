package com.lasthopesoftware.bluewater.shared.messages

import kotlinx.coroutines.flow.filterIsInstance

inline fun <reified Message : TypedMessage> TypedMessageFeed<*>.receiveMessages() = messages.filterIsInstance<Message>()
