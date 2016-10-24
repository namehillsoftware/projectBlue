package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 7/24/15.
 */
public class StoredFileUriProvider implements IFileUriProvider {
	private final StoredFileAccess storedFileAccess;
	private final Context context;
	private final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator;

	public StoredFileUriProvider(Context context, Library library, IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator) {
		this.externalStorageReadPermissionsArbitrator = externalStorageReadPermissionsArbitrator;
		storedFileAccess = new StoredFileAccess(context, library);
		this.context = context;
	}

	@Override
	public Uri getFileUri(IFile file) throws IOException {
		try {
			final StoredFile storedFile = storedFileAccess.getStoredFile(file);
			if (storedFile == null || !storedFile.isDownloadComplete()) return null;

			final File systemFile = new File(storedFile.getPath());
			if (systemFile.getAbsolutePath().contains(Environment.getExternalStorageDirectory().getAbsolutePath()) && !this.externalStorageReadPermissionsArbitrator.isReadPermissionGranted())
				return null;

			if (systemFile.exists())
				return Uri.fromFile(systemFile);
		} catch (ExecutionException | InterruptedException e) {
			LoggerFactory.getLogger(StoredFileUriProvider.class).error("There was an error while running the task to get the stored file", e);
		}

		return null;
	}
}
