package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.promises.IPromise;

public interface IFileUriProvider {
	IPromise<Uri> getFileUri(File file);
}
