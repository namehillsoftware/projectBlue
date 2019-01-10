package com.lasthopesoftware.bluewater.client.stored.library.items.files.job;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;

import java.io.File;

public class StoredFileJobStatus {
	public final File downloadedFile;
	public final StoredFile storedFile;
	public final StoredFileJobState storedFileJobState;

	public StoredFileJobStatus(@NonNull File downloadedFile, @NonNull StoredFile storedFile, @NonNull StoredFileJobState storedFileJobState) {
		this.downloadedFile = downloadedFile;
		this.storedFile = storedFile;
		this.storedFileJobState = storedFileJobState;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StoredFileJobStatus that = (StoredFileJobStatus) o;

		return storedFile.equals(that.storedFile) && storedFileJobState == that.storedFileJobState;

	}

	@Override
	public int hashCode() {
		int result = storedFile.hashCode();
		result = 31 * result + storedFileJobState.hashCode();
		return result;
	}

	public static StoredFileJobStatus empty() {
		return new StoredFileJobStatus(null, null, StoredFileJobState.None);
	}
}
