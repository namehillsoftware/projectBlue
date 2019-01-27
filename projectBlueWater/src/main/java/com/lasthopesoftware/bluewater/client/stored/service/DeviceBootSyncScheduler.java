package com.lasthopesoftware.bluewater.client.stored.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DeviceBootSyncScheduler extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

		StoredSyncService.schedule(context);
	}
}
