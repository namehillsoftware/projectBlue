package com.lasthopesoftware.resources.caching

import com.namehillsoftware.handoff.promises.Promise

class CachingPolicyFactory : ApplyExecutionPolicies {
	override fun <Input : Any, Output> applyPolicy(function: (Input) -> Promise<Output>): (Input) -> Promise<Output> {
		val functionCache = PermanentFunctionCache<Input, Output>()
		return { input -> functionCache.getOrAdd(input, function) }
	}

	override fun <In1 : Any, In2 : Any, Output> applyPolicy(function: (In1, In2) -> Promise<Output>): (In1, In2) -> Promise<Output> {
		val functionCache = PermanentFunctionCache<Pair<In1, In2>, Output>()
		return { in1, in2 -> functionCache.getOrAdd(Pair(in1, in2)) { p -> function(p.first, p.second) } }
	}

}
