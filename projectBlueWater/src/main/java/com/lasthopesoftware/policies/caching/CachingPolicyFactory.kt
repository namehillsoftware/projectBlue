package com.lasthopesoftware.policies.caching

import com.lasthopesoftware.bluewater.shared.NullBox
import com.lasthopesoftware.policies.ApplyExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

object CachingPolicyFactory : ApplyExecutionPolicies {
	override fun <Input : Any?, Output> applyPolicy(function: (Input) -> Promise<Output>): (Input) -> Promise<Output> {
		val functionCache =
			PermanentPromiseFunctionCache<NullBox<Input>, Output>()
		return { input -> functionCache.getOrAdd(NullBox(input)) { function(it.value) } }
	}

	override fun <In1 : Any?, In2 : Any?, Output> applyPolicy(function: (In1, In2) -> Promise<Output>): (In1, In2) -> Promise<Output> {
		val functionCache =
			PermanentPromiseFunctionCache<Pair<In1, In2>, Output>()
		return { in1, in2 -> functionCache.getOrAdd(Pair(in1, in2)) { (in1, in2) -> function(in1, in2) } }
	}

	override fun <In1 : Any?, In2 : Any?, In3 : Any?, Output> applyPolicy(function: (In1, In2, In3) -> Promise<Output>): (In1, In2, In3) -> Promise<Output> {
		val functionCache =
			PermanentPromiseFunctionCache<Triple<In1, In2, In3>, Output>()
		return { in1, in2, in3 -> functionCache.getOrAdd(Triple(in1, in2, in3)) { (in1, in2, in3) -> function(in1, in2, in3) } }
	}

}
