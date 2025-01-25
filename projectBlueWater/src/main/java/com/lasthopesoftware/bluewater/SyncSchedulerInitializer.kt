package com.lasthopesoftware.bluewater

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManager
import androidx.work.WorkManagerInitializer
import com.lasthopesoftware.bluewater.client.stored.sync.ScheduleSyncs
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.client.stored.sync.constraints.SyncWorkerConstraints
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.cls

class SyncSchedulerInitializer : Initializer<ScheduleSyncs> {
	override fun create(context: Context): ScheduleSyncs {
		val syncScheduler = SyncScheduler(
			WorkManager.getInstance(context),
			SyncWorkerConstraints(context.getApplicationSettingsRepository())
		)

		syncScheduler
			.promiseIsScheduled()
			.then { isScheduled -> if (!isScheduled) syncScheduler.scheduleSync() }

		return syncScheduler
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		return mutableListOf(cls<WorkManagerInitializer>())
	}
}
