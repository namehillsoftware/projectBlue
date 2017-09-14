package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;

import java.io.File;

public interface IStoredFileSystemFileProducer {
	File getFile(StoredFile storedFile);
}
