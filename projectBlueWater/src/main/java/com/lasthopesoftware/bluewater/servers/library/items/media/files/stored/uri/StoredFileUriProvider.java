package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.uri;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.uri.AbstractFileUriProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.permissions.IPermissionArbitrator;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 7/24/15.
 */
public class StoredFileUriProvider extends AbstractFileUriProvider {
	private final StoredFileAccess storedFileAccess;
	private final Context context;
	private final IPermissionArbitrator externalStorageReadPermissionsArbitrator;

	public StoredFileUriProvider(Context context, Library library, IFile file, IPermissionArbitrator externalStorageReadPermissionsArbitrator) {
		super(file);

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
			if (systemFile.getAbsolutePath().contains(Environment.getExternalStorageDirectory().getAbsolutePath()) && !this.externalStorageReadPermissionsArbitrator.isPermissionGranted())
				return null;

			if (systemFile.exists())
				return Uri.fromFile(systemFile);
		} catch (ExecutionException | InterruptedException e) {
			LoggerFactory.getLogger(StoredFileUriProvider.class).error("There was an error while running the task to get the stored file", e);
		}

		return null;
	}
}
