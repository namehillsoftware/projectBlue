package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri;

import android.net.Uri;
import android.os.Environment;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public class StoredFileUriProvider implements IFileUriProvider {
	private final IStoredFileAccess storedFileAccess;
	private final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator;

	public StoredFileUriProvider(IStoredFileAccess storedFileAccess, IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator) {
		this.externalStorageReadPermissionsArbitrator = externalStorageReadPermissionsArbitrator;
		this.storedFileAccess = storedFileAccess;
	}

	@Override
	public Promise<Uri> getFileUri(ServiceFile serviceFile) {
		return storedFileAccess
			.getStoredFile(serviceFile)
			.then(storedFile -> {
				if (storedFile == null || !storedFile.isDownloadComplete() || storedFile.getPath() == null || storedFile.getPath().isEmpty()) return null;

				final File systemFile = new File(storedFile.getPath());
				if (systemFile.getAbsolutePath().contains(Environment.getExternalStorageDirectory().getAbsolutePath()) && !this.externalStorageReadPermissionsArbitrator.isReadPermissionGranted())
					return null;

				if (systemFile.exists())
					return Uri.fromFile(systemFile);

				return null;
			});
	}
}
