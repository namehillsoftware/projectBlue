package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri;

import android.content.Context;
import android.net.Uri;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.store.Library;

import java.io.IOException;

/**
 * Created by david on 7/24/15.
 * Will get the best URI for access speed.
 */
public class BestMatchUriProvider extends AbstractFileUriProvider {
	private final Context mContext;
	private final Library mLibrary;

	public BestMatchUriProvider(Context context, Library library, IFile file) {
		super(file);

		mContext = context;
		mLibrary = library;
	}

	@Override
	public Uri getFileUri() throws IOException {
		final StoredFileUriProvider storedFileUriProvider = new StoredFileUriProvider(mContext, mLibrary, getFile());
		Uri fileUri = storedFileUriProvider.getFileUri();
		if (fileUri != null)
			return fileUri;

		final MediaFileUriProvider mediaFileUriProvider = new MediaFileUriProvider(mContext, getFile());
		fileUri = mediaFileUriProvider.getFileUri();
		if (fileUri != null)
			return fileUri;

		final RemoteFileUriProvider remoteFileUriProvider = new RemoteFileUriProvider(getFile());
		return remoteFileUriProvider.getFileUri();
	}
}
