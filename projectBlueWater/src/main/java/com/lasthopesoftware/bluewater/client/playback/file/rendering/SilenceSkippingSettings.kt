package com.lasthopesoftware.bluewater.client.playback.file.rendering

import android.content.SharedPreferences
import com.lasthopesoftware.bluewater.ApplicationConstants
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class SilenceSkippingSettings(private val sharedPreferences: SharedPreferences) : LookupSilenceSkippingSettings {
	private val minimumSilenceDuration = Duration.standardSeconds(3).toPromise()

	override fun promiseSkipSilenceIsEnabled() =
		sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.skipSilence, false).toPromise()

	override fun promiseMinimumSilenceDuration(): Promise<Duration> = minimumSilenceDuration
}
