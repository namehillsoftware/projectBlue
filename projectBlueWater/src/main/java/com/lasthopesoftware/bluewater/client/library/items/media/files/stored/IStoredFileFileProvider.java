package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;

import java.io.File;

public interface IStoredFileFileProvider {
	File getFile(StoredFile storedFile);
}
