package com.lasthopesoftware.bluewater.client.playback.file.rendering

import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

interface LookupSilenceSkippingSettings {
	fun promiseSkipSilenceIsEnabled(): Promise<Boolean>

	fun promiseMinimumSilenceDuration(): Promise<Duration>
}
