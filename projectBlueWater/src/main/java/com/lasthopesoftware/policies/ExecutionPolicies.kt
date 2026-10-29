package com.lasthopesoftware.policies

import com.namehillsoftware.handoff.promises.Promise

interface ExecutionPolicies {
	fun<Input, Output> applyPolicy(function: (Input) -> Promise<Output>): (Input) -> Promise<Output>
	fun<In1, In2, Output> applyPolicy(function: (In1, In2) -> Promise<Output>): (In1, In2) -> Promise<Output>

	fun<In1, In2, In3, Output> applyPolicy(function: (In1, In2, In3) -> Promise<Output>): (In1, In2, In3) -> Promise<Output>
}
