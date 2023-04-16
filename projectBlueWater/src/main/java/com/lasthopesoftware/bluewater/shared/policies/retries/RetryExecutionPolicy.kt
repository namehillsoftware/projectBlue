package com.lasthopesoftware.bluewater.shared.policies.retries

import com.lasthopesoftware.bluewater.shared.policies.ApplyExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

class RetryExecutionPolicy(private val retryPromises: RetryPromises) : ApplyExecutionPolicies {
	override fun <Input : Any?, Output> applyPolicy(function: (Input) -> Promise<Output>): (Input) -> Promise<Output> = { in1 ->
		retryPromises.retryOnException { function(in1) }
	}

	override fun <In1 : Any?, In2 : Any?, Output> applyPolicy(function: (In1, In2) -> Promise<Output>): (In1, In2) -> Promise<Output> = { in1, in2 ->
		retryPromises.retryOnException { function(in1, in2) }
	}

	override fun <In1 : Any?, In2 : Any?, In3 : Any?, Output> applyPolicy(function: (In1, In2, In3) -> Promise<Output>): (In1, In2, In3) -> Promise<Output> = { in1, in2, in3 ->
		retryPromises.retryOnException { function(in1, in2, in3) }
	}
}
