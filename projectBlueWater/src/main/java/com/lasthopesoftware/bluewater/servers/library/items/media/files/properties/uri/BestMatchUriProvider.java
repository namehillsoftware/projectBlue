package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;

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
	public Uri getFileUri() throws IOException {
		final StoredFileUriProvider storedFileUriProvider = new StoredFileUriProvider(context, library, getFile());
		Uri fileUri = storedFileUriProvider.getFileUri();
		if (fileUri != null)
			return fileUri;

		if (library.isUsingExistingFiles() && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
			final MediaFileUriProvider mediaFileUriProvider = new MediaFileUriProvider(context, connectionProvider, getFile());
			fileUri = mediaFileUriProvider.getFileUri();
			if (fileUri != null)
				return fileUri;
		}

		final RemoteFileUriProvider remoteFileUriProvider = new RemoteFileUriProvider(connectionProvider, getFile());
		return remoteFileUriProvider.getFileUri();
	}
}
