package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import java.io.IOException;

/**
 * Created by david on 7/24/15.
 */
public abstract class AbstractFileUriProvider {
	private final IFile file;

	public AbstractFileUriProvider(IFile file) {
		this.file = file;
	}

	public Uri getFileUri() throws IOException {
		return getFileUri(file);
	}

	protected abstract Uri getFileUri(IFile file) throws IOException;
}
