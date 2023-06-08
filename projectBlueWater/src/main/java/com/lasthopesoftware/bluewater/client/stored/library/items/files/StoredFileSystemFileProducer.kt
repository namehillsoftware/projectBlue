package com.lasthopesoftware.bluewater.client.stored.library.items.files;

import androidx.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;

import java.io.File;

public final class StoredFileSystemFileProducer implements IStoredFileSystemFileProducer {

	@Override
	public File getFile(@NonNull StoredFile storedFile) {
		return new File(storedFile.getPath());
	}

}
