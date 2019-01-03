package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;

public class StoredFileJob {
	private final ServiceFile serviceFile;
	private final StoredFile storedFile;

	public StoredFileJob(ServiceFile serviceFile, StoredFile storedFile) {
		this.serviceFile = serviceFile;
		this.storedFile = storedFile;
	}

	public ServiceFile getServiceFile() {
		return serviceFile;
	}

	public StoredFile getStoredFile() {
		return storedFile;
	}
}
