package com.lasthopesoftware.bluewater.client.stored.service.receivers.file;

import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.IStorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.storage.write.permissions.IStorageWritePermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.util.Collection;
import java.util.Collections;

public class StoredFileWritePermissionsReceiver implements ReceiveStoredFileEvent, ImmediateResponse<StoredFile, Void> {

	private final IStorageWritePermissionArbitratorForOs writePermissionArbitratorForOs;
	private final IStorageWritePermissionsRequestedBroadcaster writePermissionsRequestedBroadcaster;
	private final IStoredFileAccess storedFileAccess;

	public StoredFileWritePermissionsReceiver(
		IStorageWritePermissionArbitratorForOs writePermissionArbitratorForOs,
		IStorageWritePermissionsRequestedBroadcaster writePermissionsRequestedBroadcaster,
		IStoredFileAccess storedFileAccess) {
		this.writePermissionArbitratorForOs = writePermissionArbitratorForOs;
		this.writePermissionsRequestedBroadcaster = writePermissionsRequestedBroadcaster;
		this.storedFileAccess = storedFileAccess;
	}

	@Override
	public Promise<Void> receive(int storedFileId) {
		if (!writePermissionArbitratorForOs.isWritePermissionGranted())
			return storedFileAccess.getStoredFile(storedFileId).then(this);

		return Promise.empty();
	}

	@Override
	public Collection<String> acceptedEvents() {
		return Collections.singleton(StoredFileSynchronization.onFileWriteErrorEvent);
	}

	@Override
	public Void respond(StoredFile storedFile) {
		writePermissionsRequestedBroadcaster.sendWritePermissionsNeededBroadcast(storedFile.getLibraryId());
		return null;
	}
}
