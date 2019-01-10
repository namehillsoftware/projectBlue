package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions;

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;

public class StoredFileJobException extends Exception implements IStoredFileJobException {
	private StoredFile storedFile;

	public StoredFileJobException(StoredFile storedFile, Throwable innerException) {
		super(innerException);
		this.storedFile = storedFile;
	}

	@Override
	public StoredFile getStoredFile() {
		return storedFile;
	}
}
