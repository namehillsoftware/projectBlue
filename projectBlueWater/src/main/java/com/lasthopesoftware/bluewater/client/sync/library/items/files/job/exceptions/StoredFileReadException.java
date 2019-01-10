package com.lasthopesoftware.bluewater.client.sync.library.items.files.job.exceptions;

import com.lasthopesoftware.bluewater.client.sync.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.read.exceptions.StorageReadFileException;

import java.io.File;

/**
 * Created by david on 7/17/16.
 */
public class StoredFileReadException extends StorageReadFileException implements IStoredFileJobException {
	private final StoredFile storedFile;

	public StoredFileReadException(File file, StoredFile storedFile) {
		this(file, storedFile, null);
	}

	public StoredFileReadException(File file, StoredFile storedFile, Exception innerException) {
		super(file, innerException);
		this.storedFile = storedFile;
	}

	public StoredFile getStoredFile() { return storedFile;	}
}
