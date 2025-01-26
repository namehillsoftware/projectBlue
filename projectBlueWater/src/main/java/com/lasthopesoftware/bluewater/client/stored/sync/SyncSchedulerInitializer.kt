package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManager
import androidx.work.WorkManagerInitializer
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.client.stored.sync.constraints.SyncWorkerConstraints
import com.lasthopesoftware.bluewater.shared.cls

class SyncSchedulerInitializer : Initializer<ScheduleSyncs> {
	override fun create(context: Context): ScheduleSyncs {
		val syncScheduler = SyncScheduler(
			WorkManager.getInstance(context),
			SyncWorkerConstraints(context.applicationDependencies.applicationSettings)
		)

		syncScheduler
			.promiseIsScheduled()
			.then { isScheduled -> if (!isScheduled) syncScheduler.scheduleSync() }

		return syncScheduler
	}

	override fun dependencies(): List<Class<out Initializer<*>>> = listOf(cls<WorkManagerInitializer>())
}
