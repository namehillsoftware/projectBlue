package com.lasthopesoftware.bluewater.shared.policies

import com.namehillsoftware.handoff.promises.Promise

interface ApplyExecutionPolicies {
	fun<Input: Any, Output> applyPolicy(function: (Input) -> Promise<Output>): (Input) -> Promise<Output>
	fun<In1: Any, In2: Any, Output> applyPolicy(function: (In1, In2) -> Promise<Output>): (In1, In2) -> Promise<Output>
}
