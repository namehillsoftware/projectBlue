package com.lasthopesoftware.bluewater.client.stored.service.receivers.file;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.broadcasts.IScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class StoredFileMediaScannerNotifier implements ReceiveStoredFileEvent, ImmediateResponse<StoredFile, Void> {

	private final IStoredFileAccess storedFileAccess;
	private final IScanMediaFileBroadcaster mediaFileBroadcaster;

	public StoredFileMediaScannerNotifier(IStoredFileAccess storedFileAccess, IScanMediaFileBroadcaster mediaFileBroadcaster) {
		this.storedFileAccess = storedFileAccess;
		this.mediaFileBroadcaster = mediaFileBroadcaster;
	}

	@Override
	public Promise<Void> receive(int storedFileId) {
		return storedFileAccess.getStoredFile(storedFileId)
			.then(this);
	}

	@Override
	public Collection<String> acceptedEvents() {
		return Collections.singleton(StoredFileSynchronization.onFileDownloadedEvent);
	}

	@Override
	public Void respond(StoredFile storedFile) {
		mediaFileBroadcaster.sendScanMediaFileBroadcastForFile(new File(storedFile.getPath()));
		return null;
	}
}
