package com.lasthopesoftware.storage.write.exceptions;

import java.io.File;
import java.io.IOException;

public class StorageWriteFileException extends IOException {

	private final File file;

	public StorageWriteFileException(File file) {
		this(file, null);
	}

	public StorageWriteFileException(File file, Exception innerException) {
		super("There was an error writing the serviceFile " + file + ".", innerException);

		this.file = file;
	}

	public File getFile() { return file; }
}
