package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DeviceBootSyncScheduler : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (Intent.ACTION_BOOT_COMPLETED != intent.action) return

		SyncWorker.promiseIsScheduled(context)
			.then { isScheduled ->
				if (!isScheduled) SyncWorker.scheduleSync(context)
			}
	}
}
