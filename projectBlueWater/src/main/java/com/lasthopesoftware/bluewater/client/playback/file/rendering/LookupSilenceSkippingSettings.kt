package com.lasthopesoftware.bluewater.client.playback.file.rendering

import com.namehillsoftware.handoff.promises.Promise

interface LookupSilenceSkippingSettings {
	fun promiseSkipSilenceIsEnabled(): Promise<Boolean>
}
