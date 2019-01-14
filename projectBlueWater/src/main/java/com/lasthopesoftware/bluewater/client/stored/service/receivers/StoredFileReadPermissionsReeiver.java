package com.lasthopesoftware.bluewater.client.stored.service.receivers;

import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.IStorageReadPermissionsRequestedBroadcast;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

public class StoredFileReadPermissionsReeiver implements ReceiveStoredFileEvent, ImmediateResponse<StoredFile, Void> {

	private final IStorageReadPermissionArbitratorForOs readPermissionArbitratorForOs;
	private final IStorageReadPermissionsRequestedBroadcast readPermissionsRequestedBroadcast;
	private final IStoredFileAccess storedFileAccess;

	public StoredFileReadPermissionsReeiver(IStorageReadPermissionArbitratorForOs readPermissionArbitratorForOs, IStorageReadPermissionsRequestedBroadcast readPermissionsRequestedBroadcast, IStoredFileAccess storedFileAccess) {
		this.readPermissionArbitratorForOs = readPermissionArbitratorForOs;
		this.readPermissionsRequestedBroadcast = readPermissionsRequestedBroadcast;
		this.storedFileAccess = storedFileAccess;
	}

	@Override
	public void receive(int storedFileId) {
		if (!readPermissionArbitratorForOs.isReadPermissionGranted())
			storedFileAccess.getStoredFile(storedFileId).then(this);
	}

	@Override
	public boolean isAcceptable(String event) {
		return StoredFileSynchronization.onFileReadErrorEvent.equals(event);
	}

	@Override
	public Void respond(StoredFile storedFile) {
		readPermissionsRequestedBroadcast.sendReadPermissionsRequestedBroadcast(storedFile.getLibraryId());
		return null;
	}
}
