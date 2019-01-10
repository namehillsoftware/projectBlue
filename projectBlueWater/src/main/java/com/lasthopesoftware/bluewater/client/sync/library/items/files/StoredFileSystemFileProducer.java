package com.lasthopesoftware.bluewater.client.sync.library.items.files;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.sync.library.items.files.repository.StoredFile;

import java.io.File;

public final class StoredFileSystemFileProducer implements IStoredFileSystemFileProducer {

	@Override
	public File getFile(@NonNull StoredFile storedFile) {
		return new File(storedFile.getPath());
	}

}
