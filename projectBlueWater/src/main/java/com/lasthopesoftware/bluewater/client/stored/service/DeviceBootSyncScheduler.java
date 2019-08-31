package com.lasthopesoftware.bluewater.client.stored.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.lasthopesoftware.bluewater.client.stored.scheduling.SyncSchedulingWorker;

public class DeviceBootSyncScheduler extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

		SyncSchedulingWorker.promiseIsScheduled(context)
			.then(isScheduled -> {
				if (!isScheduled)
					SyncSchedulingWorker.scheduleSync(context);

				return null;
			});
	}
}
