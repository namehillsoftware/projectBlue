package com.lasthopesoftware.policies.ratelimiting

import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise

interface RateLimitPromises<T> {
	fun enqueuePromise(factory: () -> Promise<T>): Promise<T>
	fun <Progress> enqueueProgressingPromise(factory: () -> ProgressingPromise<Progress, T>): ProgressingPromise<Progress, T>
}
