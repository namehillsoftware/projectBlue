package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.access;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.namehillsoftware.handoff.promises.Promise;


public class CachedFileUriProvider implements IFileUriProvider {

	private final ICachedFilesProvider cachedFilesProvider;

	public CachedFileUriProvider(ICachedFilesProvider cachedFilesProvider) {
		this.cachedFilesProvider = cachedFilesProvider;
	}

	@Override
	public Promise<Uri> promiseFileUri(ServiceFile serviceFile) {
		return Promise.empty();
	}
}
