package com.lasthopesoftware.bluewater.client.stored.library.items.files.job;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;

import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StoredFileJob that = (StoredFileJob) o;
		return Objects.equals(serviceFile, that.serviceFile) &&
			Objects.equals(storedFile, that.storedFile);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serviceFile, storedFile);
	}
}
