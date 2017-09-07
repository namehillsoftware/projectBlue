package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;

import java.io.File;

public class StoredFileFileProvider implements IStoredFileFileProvider {
	@Override
	public File getFile(@NonNull StoredFile storedFile) {
		return new File(storedFile.getPath());
	}
}
