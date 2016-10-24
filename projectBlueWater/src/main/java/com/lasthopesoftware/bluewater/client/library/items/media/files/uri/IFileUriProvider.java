package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import java.io.IOException;

/**
 * Created by david on 7/24/15.
 */
public interface IFileUriProvider {
	Uri getFileUri(IFile file) throws IOException;
}
