package com.lasthopesoftware.bluewater.shared.policies.ratelimiting

import com.namehillsoftware.handoff.promises.Promise

interface RateLimitPromises<T> {
	fun limit(factory: () -> Promise<T>): Promise<T>
}
