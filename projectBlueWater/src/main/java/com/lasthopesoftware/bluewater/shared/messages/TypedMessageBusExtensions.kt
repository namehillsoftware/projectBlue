package com.lasthopesoftware.bluewater.shared.messages

inline fun <S : TypedMessage, reified M : S> RegisterForTypedMessages<S>.registerReceiver(noinline receiver: (M) -> Unit): AutoCloseable =
	registerReceiver(M::class.java, receiver)
