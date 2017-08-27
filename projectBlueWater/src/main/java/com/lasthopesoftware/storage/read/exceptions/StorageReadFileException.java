package com.lasthopesoftware.storage.read.exceptions;

import java.io.File;
import java.io.IOException;

public class StorageReadFileException extends IOException {
	private final File file;

	public StorageReadFileException(File file) {
		this(file, null);
	}

	public StorageReadFileException(File file, Exception innerException) {
		super("There was an error reading the serviceFile " + file + ".", innerException);

		this.file = file;
	}

	public File getFile() { return file; }
}
