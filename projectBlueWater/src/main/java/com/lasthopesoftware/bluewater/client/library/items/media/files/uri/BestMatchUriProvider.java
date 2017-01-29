package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.content.Context;
import android.net.Uri;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri.StoredFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;

import java.io.IOException;

/**
 * Created by david on 7/24/15.
 * Will get the best URI for access speed.
 */
public class BestMatchUriProvider implements IFileUriProvider {
	private final Context context;
	private final Library library;
	private final IConnectionProvider connectionProvider;

	public BestMatchUriProvider(Context context, IConnectionProvider connectionProvider, Library library) {
		this.context = context;
		this.library = library;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Uri getFileUri(IFile file) throws IOException {
		final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator = new ExternalStorageReadPermissionsArbitratorForOs(context);

		final StoredFileUriProvider storedFileUriProvider = new StoredFileUriProvider(context, library, externalStorageReadPermissionsArbitrator);
		Uri fileUri = storedFileUriProvider.getFileUri(file);
		if (fileUri != null)
			return fileUri;

		if (library.isUsingExistingFiles()) {
			final MediaFileUriProvider mediaFileUriProvider = new MediaFileUriProvider(context, new MediaQueryCursorProvider(context, connectionProvider), externalStorageReadPermissionsArbitrator);
			fileUri = mediaFileUriProvider.getFileUri(file);
			if (fileUri != null)
				return fileUri;
		}

		final RemoteFileUriProvider remoteFileUriProvider = new RemoteFileUriProvider(connectionProvider);
		return remoteFileUriProvider.getFileUri(file);
	}
}
