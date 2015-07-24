package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri;

import android.content.Context;
import android.net.Uri;

import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.store.Library;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 7/24/15.
 */
public class StoredFileUriProvider extends AbstractFileUriProvider {
	private final Context mContext;
	private final StoredFileAccess mStoredFileAccess;

	public StoredFileUriProvider(Context context, Library library, IFile file) {
		super(file);

		mContext = context;
		mStoredFileAccess = new StoredFileAccess(mContext, library);
	}

	@Override
	public Uri getFileUri() throws IOException {
		try {
			return Uri.fromFile(new File(mStoredFileAccess.getStoredFile(getFile()).getPath()));
		} catch (ExecutionException | InterruptedException e) {
			LoggerFactory.getLogger(StoredFileUriProvider.class).error("There was an error while running the task to get the stored file", e);
		}

		return null;
	}
}
