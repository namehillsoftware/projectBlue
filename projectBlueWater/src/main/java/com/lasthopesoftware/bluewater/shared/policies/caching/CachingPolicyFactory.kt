package com.lasthopesoftware.bluewater.shared.policies.caching

import com.lasthopesoftware.bluewater.shared.policies.ApplyExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

class CachingPolicyFactory : ApplyExecutionPolicies {
	override fun <Input : Any, Output> applyPolicy(function: (Input) -> Promise<Output>): (Input) -> Promise<Output> {
		val functionCache = PermanentPromiseFunctionCache<Input, Output>()
		return { input -> functionCache.getOrAdd(input, function) }
	}

	override fun <In1 : Any, In2 : Any, Output> applyPolicy(function: (In1, In2) -> Promise<Output>): (In1, In2) -> Promise<Output> {
		val functionCache = PermanentPromiseFunctionCache<Pair<In1, In2>, Output>()
		return { in1, in2 -> functionCache.getOrAdd(Pair(in1, in2)) { (in1, in2) -> function(in1, in2) } }
	}
}
