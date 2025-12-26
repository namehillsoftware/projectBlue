package com.lasthopesoftware.policies.ratelimiting

import com.lasthopesoftware.policies.ExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

class RateLimitingExecutionPolicy(private val rate: Int) : ExecutionPolicies {
	override fun <Input, Output> applyPolicy(function: (Input) -> Promise<Output>): (Input) -> Promise<Output> {
		val rateLimiter = PromisingRateLimiter<Output>(rate)
		return { i -> rateLimiter.enqueuePromise { function(i) } }
	}

	override fun <In1, In2, Output> applyPolicy(function: (In1, In2) -> Promise<Output>): (In1, In2) -> Promise<Output> {
		val rateLimiter = PromisingRateLimiter<Output>(rate)
		return { in1, in2 -> rateLimiter.enqueuePromise { function(in1, in2) } }
	}

	override fun <In1, In2, In3, Output> applyPolicy(function: (In1, In2, In3) -> Promise<Output>): (In1, In2, In3) -> Promise<Output> {
		val rateLimiter = PromisingRateLimiter<Output>(rate)
		return { in1, in2, in3 -> rateLimiter.enqueuePromise { function(in1, in2, in3) } }
	}
}
