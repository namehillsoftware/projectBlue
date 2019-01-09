package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.job.exceptions;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.storage.write.exceptions.StorageWriteFileException;

import java.io.File;

/**
 * Created by david on 7/17/16.
 */
public class StoredFileWriteException extends StorageWriteFileException implements IStoredFileJobException {
	private final StoredFile storedFile;

	public StoredFileWriteException(File file, StoredFile storedFile) {
		this(file, storedFile, null);
	}

	public StoredFileWriteException(File file, StoredFile storedFile, Exception innerException) {
		super(file, innerException);

		this.storedFile = storedFile;
	}

	public StoredFile getStoredFile() { return storedFile;	}

}
