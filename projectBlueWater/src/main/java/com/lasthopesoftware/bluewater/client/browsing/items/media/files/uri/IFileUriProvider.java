package com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface IFileUriProvider {
	Promise<Uri> promiseFileUri(ServiceFile serviceFile);
}
