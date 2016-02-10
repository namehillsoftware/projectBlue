package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

import java.io.IOException;

/**
 * Created by david on 7/24/15.
 */
abstract class AbstractFileUriProvider {
	private final IFile mFile;

	AbstractFileUriProvider(IFile file) {
		mFile = file;
	}

	IFile getFile() {
		return mFile;
	}

	public abstract Uri getFileUri() throws IOException;
}
