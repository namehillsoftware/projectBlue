package com.lasthopesoftware.resources.closables

import com.lasthopesoftware.promises.extensions.instead
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.EventualAction
import com.namehillsoftware.handoff.promises.response.ImmediateAction

interface PromisingCloseable {
	fun promiseClose(): Promise<Unit>
}

inline fun <T : AutoCloseable, Result> T.eventuallyUse(usage: (T) -> Promise<Result>): Promise<Result> {
	val use = usage(this)
	return object : Promise.Proxy<Result>(), ImmediateAction {
		init {
			proxy(use)
			use.must(this)
		}

		override fun act() {
			close()
		}
	}
}

inline fun <T : PromisingCloseable, Result> T.eventuallyUse(usage: (T) -> Promise<Result>): Promise<Result> {
	val use = usage(this)
	return object : Promise.Proxy<Result>(), EventualAction {
		init {
			proxy(use)
			use.inevitably(this)
		}

		override fun promiseAction(): Promise<*> = promiseClose()
	}
}

inline fun <T : PromisingCloseable, Result> Promise<T>.eventuallyUse(crossinline usage: (T) -> Promise<Result>): Promise<Result> = eventually { resource ->
	resource.eventuallyUse(usage)
}

inline fun <T : PromisingCloseable, Result> T.thenUse(usage: (T) -> Result): Promise<Result> =
	try {
		val use = usage(this)
		promiseClose().instead(use)
	} catch (e: Throwable) {
		promiseClose().instead(e)
	}
