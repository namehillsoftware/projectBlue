package com.lasthopesoftware.resources.closables

import com.namehillsoftware.handoff.promises.Promise

interface PromisingCloseable {
	fun promiseClose(): Promise<Unit>
}

inline fun <T : PromisingCloseable, Result> T.useEventually(usage: (T) -> Promise<Result>): Promise<Result> =
	usage(this).inevitably { promiseClose() }
