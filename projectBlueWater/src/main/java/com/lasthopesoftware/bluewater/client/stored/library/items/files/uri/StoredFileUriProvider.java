package com.lasthopesoftware.bluewater.client.stored.library.items.files.uri;

import android.net.Uri;
import android.os.Environment;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public class StoredFileUriProvider implements IFileUriProvider {
	private final IStoredFileAccess storedFileAccess;
	private final ISelectedBrowserLibraryProvider selectedBrowserLibraryProvider;
	private final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator;

	public StoredFileUriProvider(ISelectedBrowserLibraryProvider selectedBrowserLibraryProvider, IStoredFileAccess storedFileAccess, IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator) {
		this.selectedBrowserLibraryProvider = selectedBrowserLibraryProvider;
		this.externalStorageReadPermissionsArbitrator = externalStorageReadPermissionsArbitrator;
		this.storedFileAccess = storedFileAccess;
	}

	@Override
	public Promise<Uri> promiseFileUri(ServiceFile serviceFile) {
		return selectedBrowserLibraryProvider
			.getBrowserLibrary()
			.eventually(library -> storedFileAccess.getStoredFile(library, serviceFile))
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
