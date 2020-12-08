package com.lasthopesoftware.bluewater.client.playback.engine

import com.namehillsoftware.handoff.promises.Promise

interface ChangePlaybackContinuity {
	fun playRepeatedly(): Promise<Unit>
	fun playToCompletion(): Promise<Unit>
}
