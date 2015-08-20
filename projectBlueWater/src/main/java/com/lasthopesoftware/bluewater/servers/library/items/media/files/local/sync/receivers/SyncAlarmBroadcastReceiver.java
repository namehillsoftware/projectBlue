package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.service.ItemSyncService;

/**
 * Created by david on 8/19/15.
 */
public class SyncAlarmBroadcastReceiver extends BroadcastReceiver {
	public static final String scheduledSyncIntent = "com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.doScheduledSync";

	@Override
	public void onReceive(Context context, Intent intent) {
		ItemSyncService.doSync(context);
	}
}
