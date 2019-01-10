package com.lasthopesoftware.bluewater.client.sync.library.items.files;

import com.lasthopesoftware.bluewater.client.sync.library.items.files.repository.StoredFile;

import java.io.File;

public interface IStoredFileSystemFileProducer {
	File getFile(StoredFile storedFile);
}
