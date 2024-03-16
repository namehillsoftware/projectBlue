package com.lasthopesoftware.policies.retries

import com.lasthopesoftware.policies.ApplyExecutionPolicies
import com.lasthopesoftware.policies.ratelimiting.PromisingRateLimiter
import com.namehillsoftware.handoff.promises.Promise

class RateLimitingExecutionPolicy(private val rate: Int) : ApplyExecutionPolicies {
	override fun <Input, Output> applyPolicy(function: (Input) -> Promise<Output>): (Input) -> Promise<Output> {
		val rateLimiter = PromisingRateLimiter<Output>(rate)
		return { i -> rateLimiter.limit { function(i) } }
	}

	override fun <In1, In2, Output> applyPolicy(function: (In1, In2) -> Promise<Output>): (In1, In2) -> Promise<Output> {
		val rateLimiter = PromisingRateLimiter<Output>(rate)
		return { in1, in2 -> rateLimiter.limit { function(in1, in2) } }
	}

	override fun <In1, In2, In3, Output> applyPolicy(function: (In1, In2, In3) -> Promise<Output>): (In1, In2, In3) -> Promise<Output> {
		val rateLimiter = PromisingRateLimiter<Output>(rate)
		return { in1, in2, in3 -> rateLimiter.limit { function(in1, in2, in3) } }
	}
}
