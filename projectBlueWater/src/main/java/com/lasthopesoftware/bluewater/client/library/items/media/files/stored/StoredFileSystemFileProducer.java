package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.namehillsoftware.lazyj.Lazy;

import java.io.File;

public final class StoredFileSystemFileProducer implements IStoredFileSystemFileProducer {

	private static final Lazy<StoredFileSystemFileProducer> lazyInstance = new Lazy<>(StoredFileSystemFileProducer::new);

	private StoredFileSystemFileProducer() {}

	@Override
	public File getFile(@NonNull StoredFile storedFile) {
		return new File(storedFile.getPath());
	}

	public static StoredFileSystemFileProducer getInstance() {
		return lazyInstance.getObject();
	}
}
