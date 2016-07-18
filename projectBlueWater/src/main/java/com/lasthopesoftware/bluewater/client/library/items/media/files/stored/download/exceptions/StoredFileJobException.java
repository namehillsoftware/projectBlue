package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;

/**
 * Created by david on 7/17/16.
 */
public class StoredFileJobException extends Exception implements IStoredFileJobException {
	private StoredFile storedFile;

	public StoredFileJobException(StoredFile storedFile, Exception innerException) {
		super(innerException);
		this.storedFile = storedFile;
	}

	@Override
	public StoredFile getStoredFile() {
		return storedFile;
	}
}
