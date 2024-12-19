package com.lasthopesoftware.promises.extensions

import com.namehillsoftware.handoff.promises.Promise

sealed interface ContinuableResult<Progress>

class HaltedResult<Progress> : ContinuableResult<Progress>

class ContinuingResult<Progress>(
	val current: Progress,
	val next: Promise<ContinuableResult<Progress>>
) : ContinuableResult<Progress>
