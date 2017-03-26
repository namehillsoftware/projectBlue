package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;

import java.io.File;

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
	public IPromise<Uri> getFileUri(com.lasthopesoftware.bluewater.client.library.items.media.files.File file) {
		return
			storedFileAccess
				.getStoredFile(file)
				.then(storedFile -> {
					if (storedFile == null || !storedFile.isDownloadComplete()) return null;

					final File systemFile = new File(storedFile.getPath());
					if (systemFile.getAbsolutePath().contains(Environment.getExternalStorageDirectory().getAbsolutePath()) && !this.externalStorageReadPermissionsArbitrator.isReadPermissionGranted())
						return null;

					if (systemFile.exists())
						return Uri.fromFile(systemFile);

					return null;
				});
	}
}
