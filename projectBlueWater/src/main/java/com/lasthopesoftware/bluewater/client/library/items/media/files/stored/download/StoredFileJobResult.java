package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;

import java.io.File;

public class StoredFileJobResult {
	public final File downloadedFile;
	public final StoredFile storedFile;
	public final StoredFileJobResultOptions storedFileJobResult;

	public StoredFileJobResult(@NonNull File downloadedFile, @NonNull StoredFile storedFile, @NonNull StoredFileJobResultOptions storedFileJobResult) {
		this.downloadedFile = downloadedFile;
		this.storedFile = storedFile;
		this.storedFileJobResult = storedFileJobResult;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StoredFileJobResult that = (StoredFileJobResult) o;

		return storedFile.equals(that.storedFile) && storedFileJobResult == that.storedFileJobResult;

	}

	@Override
	public int hashCode() {
		int result = storedFile.hashCode();
		result = 31 * result + storedFileJobResult.hashCode();
		return result;
	}
}
