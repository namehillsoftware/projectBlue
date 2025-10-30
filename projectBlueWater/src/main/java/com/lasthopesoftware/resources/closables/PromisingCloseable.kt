package com.lasthopesoftware.resources.closables

import com.namehillsoftware.handoff.promises.Promise

interface PromisingCloseable {
	fun promiseClose(): Promise<Unit>
}

inline fun <T : AutoCloseable, Result> T.eventuallyUse(usage: (T) -> Promise<Result>): Promise<Result> {
	val use = usage(this)
	return Promise.Proxy {
		use.also(it::doCancel).must(::close)
	}
}

inline fun <T : PromisingCloseable, Result> T.eventuallyUse(usage: (T) -> Promise<Result>): Promise<Result> {
	val use = usage(this)
	return Promise.Proxy {
		use.also(it::doCancel).inevitably { promiseClose() }
	}
}

inline fun <T : PromisingCloseable, Result> T.thenUse(usage: (T) -> Result): Promise<Result> =
	try {
		val use = usage(this)
		promiseClose().then { use }
	} catch (e: Throwable) {
		promiseClose().then { throw e }
	}
