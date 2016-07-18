package com.lasthopesoftware.storage.read.exceptions;

import java.io.File;
import java.io.IOException;

/**
 * Created by david on 7/17/16.
 */
public class StorageReadFileException extends IOException {
	private final File file;

	public StorageReadFileException(File file) {
		this(file, null);
	}

	public StorageReadFileException(File file, Exception innerException) {
		super("There was an error reading the file " + file + ".", innerException);

		this.file = file;
	}

	public File getFile() { return file; }
}
