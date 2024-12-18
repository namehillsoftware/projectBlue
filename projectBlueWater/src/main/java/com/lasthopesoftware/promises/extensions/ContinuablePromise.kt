package com.lasthopesoftware.promises.extensions

import com.namehillsoftware.handoff.promises.Promise

interface ContinuablePromise<Progress> {
	val current: Progress
	val next: Promise<ContinuablePromise<Progress>>?
}
