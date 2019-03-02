package com.lasthopesoftware.bluewater.client.stored.service.receivers.file;

import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.IStorageReadPermissionsRequestedBroadcast;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.util.Collection;
import java.util.Collections;

public class StoredFileReadPermissionsReceiver implements ReceiveStoredFileEvent, ImmediateResponse<StoredFile, Void> {

	private final IStorageReadPermissionArbitratorForOs readPermissionArbitratorForOs;
	private final IStorageReadPermissionsRequestedBroadcast readPermissionsRequestedBroadcast;
	private final IStoredFileAccess storedFileAccess;

	public StoredFileReadPermissionsReceiver(IStorageReadPermissionArbitratorForOs readPermissionArbitratorForOs, IStorageReadPermissionsRequestedBroadcast readPermissionsRequestedBroadcast, IStoredFileAccess storedFileAccess) {
		this.readPermissionArbitratorForOs = readPermissionArbitratorForOs;
		this.readPermissionsRequestedBroadcast = readPermissionsRequestedBroadcast;
		this.storedFileAccess = storedFileAccess;
	}

	@Override
	public Promise<Void> receive(int storedFileId) {
		if (!readPermissionArbitratorForOs.isReadPermissionGranted())
			return storedFileAccess.getStoredFile(storedFileId).then(this);

		return Promise.empty();
	}

	@Override
	public Collection<String> acceptedEvents() {
		return Collections.singleton(StoredFileSynchronization.onFileReadErrorEvent);
	}

	@Override
	public Void respond(StoredFile storedFile) {
		readPermissionsRequestedBroadcast.sendReadPermissionsRequestedBroadcast(storedFile.getLibraryId());
		return null;
	}
}
