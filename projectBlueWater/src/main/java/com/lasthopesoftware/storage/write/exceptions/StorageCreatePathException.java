package com.lasthopesoftware.storage.write.exceptions;

import java.io.File;
import java.io.IOException;

/**
 * Created by david on 7/17/16.
 */
public class StorageCreatePathException extends IOException {
	public StorageCreatePathException(File file) {
		super("There was an error creating the path " + file + ".");
	}
}
