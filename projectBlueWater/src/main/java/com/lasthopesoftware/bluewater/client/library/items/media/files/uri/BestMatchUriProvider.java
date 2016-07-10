package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.content.Context;
import android.net.Uri;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri.StoredFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.permissions.storage.read.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.permissions.storage.read.IStorageReadPermissionArbitratorForOs;

import java.io.IOException;

/**
 * Created by david on 7/24/15.
 * Will get the best URI for access speed.
 */
public class BestMatchUriProvider extends AbstractFileUriProvider {
	private final Context context;
	private final Library library;
	private final ConnectionProvider connectionProvider;

	public BestMatchUriProvider(Context context, ConnectionProvider connectionProvider, Library library, IFile file) {
		super(file);

		this.context = context;
		this.library = library;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Uri getFileUri(IFile file) throws IOException {
		final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator = new ExternalStorageReadPermissionsArbitratorForOs(context);

		final StoredFileUriProvider storedFileUriProvider = new StoredFileUriProvider(context, library, file, externalStorageReadPermissionsArbitrator);
		Uri fileUri = storedFileUriProvider.getFileUri();
		if (fileUri != null)
			return fileUri;

		if (library.isUsingExistingFiles()) {
			final MediaFileUriProvider mediaFileUriProvider = new MediaFileUriProvider(context, new MediaQueryCursorProvider(context, connectionProvider), file, externalStorageReadPermissionsArbitrator);
			fileUri = mediaFileUriProvider.getFileUri();
			if (fileUri != null)
				return fileUri;
		}

		final RemoteFileUriProvider remoteFileUriProvider = new RemoteFileUriProvider(connectionProvider, file);
		return remoteFileUriProvider.getFileUri();
	}
}
