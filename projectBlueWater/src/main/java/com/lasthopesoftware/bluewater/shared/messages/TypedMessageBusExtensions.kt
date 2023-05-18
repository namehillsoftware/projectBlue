package com.lasthopesoftware.bluewater.shared.messages

import android.os.Handler
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.CancellationException
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

inline fun <reified M : ApplicationMessage> RegisterForApplicationMessages.promiseReceivedMessage(crossinline predicate: (M) -> Boolean = { true }) =
	promiseReceivedMessage<ApplicationMessage, M>(predicate)

inline fun <S : TypedMessage, reified M : S> RegisterForTypedMessages<S>.promiseReceivedMessage(crossinline predicate: (M) -> Boolean = { true }): Promise<M> = object : Promise<M>(), (M) -> Unit, Runnable {
	private val closeable = registerReceiver(this)

	init {
		respondToCancellation(this)
	}

	override fun invoke(p1: M) {
		if (predicate(p1)) {
			resolve(p1)
			closeable.close()
		}
	}

	override fun run() {
		closeable.close()
		reject(CancellationException("No longer waiting for message of type ${M::class.java.name}"))
	}
}
