package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.messenger.promise.Promise;

public interface IFileUriProvider {
	Promise<Uri> getFileUri(ServiceFile serviceFile);
}
