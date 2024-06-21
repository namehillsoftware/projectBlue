package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies

class DeviceBootSyncScheduler : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (Intent.ACTION_BOOT_COMPLETED != intent.action) return

		val syncScheduler = context.applicationDependencies.syncScheduler
		syncScheduler.promiseIsScheduled()
			.then { isScheduled ->
				if (!isScheduled) syncScheduler.scheduleSync()
			}
	}
}
