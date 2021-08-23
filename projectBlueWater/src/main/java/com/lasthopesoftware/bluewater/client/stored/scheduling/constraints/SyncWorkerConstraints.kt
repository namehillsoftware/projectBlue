package com.lasthopesoftware.bluewater.client.stored.scheduling.constraints

import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.NetworkType
import com.lasthopesoftware.bluewater.settings.repository.ApplicationConstants

class SyncWorkerConstraints(private val sharedPreferences: SharedPreferences) : ConstrainSyncWork {
	override fun getCurrentConstraints(): Constraints {
		val isSyncOnWifiOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, false)
		return Constraints.Builder()
			.setRequiredNetworkType(if (isSyncOnWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
			.setRequiresCharging(sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false))
			.build()
	}
}
