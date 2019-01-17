package com.lasthopesoftware.bluewater.client.stored.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;

public class StoredFileBroadcastReceiver extends BroadcastReceiver {

	private final ReceiveStoredFileEvent receiveStoredFileEvent;

	public StoredFileBroadcastReceiver(ReceiveStoredFileEvent receiveStoredFileEvent) {
		this.receiveStoredFileEvent = receiveStoredFileEvent;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final int storedFileId = intent.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1);
		if (storedFileId > 0)
			receiveStoredFileEvent.receive(storedFileId);
	}
}
