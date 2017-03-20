package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 7/24/15.
 */
public class StoredFileUriProvider implements IFileUriProvider {
	private final StoredFileAccess storedFileAccess;
	private final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator;

	public StoredFileUriProvider(Context context, Library library, IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator) {
		this.externalStorageReadPermissionsArbitrator = externalStorageReadPermissionsArbitrator;
		storedFileAccess = new StoredFileAccess(context, library);
	}

	@Override
	public IPromise<Uri> getFileUri(IFile file) {
		try {
			final StoredFile storedFile = storedFileAccess.getStoredFile(file);
			if (storedFile == null || !storedFile.isDownloadComplete()) return Promise.empty();

			final File systemFile = new File(storedFile.getPath());
			if (systemFile.getAbsolutePath().contains(Environment.getExternalStorageDirectory().getAbsolutePath()) && !this.externalStorageReadPermissionsArbitrator.isReadPermissionGranted())
				return Promise.empty();

			if (systemFile.exists())
				return new Promise<>(Uri.fromFile(systemFile));
		} catch (ExecutionException | InterruptedException e) {
			LoggerFactory.getLogger(StoredFileUriProvider.class).error("There was an error while running the task to get the stored file", e);
		}

		return Promise.empty();
	}
}
