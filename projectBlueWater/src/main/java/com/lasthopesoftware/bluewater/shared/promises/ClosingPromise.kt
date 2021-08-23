package com.lasthopesoftware.bluewater.shared.promises

import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.handoff.promises.response.ImmediateAction

fun <C : AutoCloseable, T> C.promiseUse(block: (C) -> Promise<T>): Promise<T> = Promise(ClosingMessenger(this, block))

private class ClosingMessenger<C : AutoCloseable, T>(private val closeable: C, private val block: (C) -> Promise<T>): MessengerOperator<T>, ImmediateAction {

	override fun send(messenger: Messenger<T>) =
		PromiseProxy(messenger)
			.proxy(block(closeable).must(this))

	override fun act() = closeable.close()
}
