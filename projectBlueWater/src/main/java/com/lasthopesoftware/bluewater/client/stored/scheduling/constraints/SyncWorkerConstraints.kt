package com.lasthopesoftware.bluewater.client.stored.scheduling.constraints

import androidx.work.Constraints
import androidx.work.NetworkType
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

class SyncWorkerConstraints(private val applicationSettings: HoldApplicationSettings) : ConstrainSyncWork {
	override val currentConstraints: Promise<Constraints>
		get() = applicationSettings.promiseApplicationSettings()
			.then { a ->
				Constraints.Builder()
					.setRequiredNetworkType(if (a.isSyncOnWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
					.setRequiresCharging(a.isSyncOnPowerOnly)
					.build()
			}
}
