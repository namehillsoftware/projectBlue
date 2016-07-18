package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;

import java.io.File;

/**
 * Created by david on 7/17/16.
 */
public class StoredFileJobResult {
	public final File downloadedFile;
	public final StoredFile storedFile;
	public final StoredFileJobResultOptions storedFileJobResult;

	public StoredFileJobResult(@NonNull File downloadedFile, @NonNull StoredFile storedFile, @NonNull StoredFileJobResultOptions storedFileJobResult) {
		this.downloadedFile = downloadedFile;
		this.storedFile = storedFile;
		this.storedFileJobResult = storedFileJobResult;
	}
}
