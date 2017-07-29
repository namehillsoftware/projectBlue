package com.lasthopesoftware.storage.write.exceptions;

import java.io.File;
import java.io.IOException;

/**
 * Created by david on 7/17/16.
 */
public class StorageWriteFileException extends IOException {

	private final File file;

	public StorageWriteFileException(File file) {
		this(file, null);
	}

	public StorageWriteFileException(File file, Exception innerException) {
		super("There was an excuse writing the serviceFile " + file + ".", innerException);

		this.file = file;
	}

	public File getFile() { return file; }
}
