package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.promises.IPromise;

public interface IFileUriProvider {
	IPromise<Uri> getFileUri(ServiceFile serviceFile);
}
