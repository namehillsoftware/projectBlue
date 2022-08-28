package com.lasthopesoftware.bluewater.client.stored.library.items.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface IStoredFileUriProvider {
	Promise<Uri> promiseStoredFileUri(ServiceFile serviceFile);
}
