package com.lasthopesoftware.bluewater.client.stored.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.lasthopesoftware.bluewater.client.stored.service.notifications.PostSyncNotification;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;

import java.util.Collection;
import java.util.Collections;

public class SyncStartedReceiver extends BroadcastReceiver {

	private final PostSyncNotification syncNotification;

	public SyncStartedReceiver(PostSyncNotification syncNotification) {
		this.syncNotification = syncNotification;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		syncNotification.notify(null);
	}

	public Collection<String> acceptedEvents() {
		return Collections.singleton(StoredFileSynchronization.onSyncStartEvent);
	}
}
