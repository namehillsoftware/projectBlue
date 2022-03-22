package com.lasthopesoftware.bluewater.shared.messages

inline fun <S : TypedMessage, reified M : S> RegisterForTypedMessages<S>.registerReceiver(noinline receiver: (M) -> Unit): AutoCloseable =
	registerForClass(M::class.java, receiver)
