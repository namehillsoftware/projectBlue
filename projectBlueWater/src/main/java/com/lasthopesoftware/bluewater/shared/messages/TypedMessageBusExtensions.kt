package com.lasthopesoftware.bluewater.shared.messages

import android.os.Handler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun <S: TypedMessage, M : S> RegisterForTypedMessages<S>.registerOnHandler(messageClass: Class<M>, handler: Handler, receiver: (M) -> Unit): AutoCloseable =
	registerForClass(messageClass) {
		if (Thread.currentThread() == handler.looper.thread) receiver(it)
		else handler.post { receiver(it) }
	}

inline fun <S : TypedMessage, reified M : S> RegisterForTypedMessages<S>.registerReceiver(handler: Handler, noinline receiver: (M) -> Unit): AutoCloseable =
	registerOnHandler(M::class.java, handler, receiver)

inline fun <S : TypedMessage, reified M : S> RegisterForTypedMessages<S>.registerReceiver(noinline receiver: (M) -> Unit): AutoCloseable =
	registerForClass(M::class.java, receiver)

inline fun <S : TypedMessage, reified M : S> RegisterForTypedMessages<S>.registerReceiver(scope: CoroutineScope, noinline receiver: (M) -> Unit): AutoCloseable =
	registerForClass(M::class.java) {
		scope.launch {
			receiver(it)
		}
	}
