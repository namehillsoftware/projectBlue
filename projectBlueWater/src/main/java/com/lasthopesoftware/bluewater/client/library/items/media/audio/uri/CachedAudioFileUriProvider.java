package com.lasthopesoftware.bluewater.client.library.items.media.audio.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.access.ICachedFilesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.RemoteFileUriProvider;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;


public class CachedAudioFileUriProvider implements IFileUriProvider {

	private final RemoteFileUriProvider remoteFileUriProvider;
	private final ICachedFilesProvider cachedFilesProvider;

	public CachedAudioFileUriProvider(RemoteFileUriProvider remoteFileUriProvider, ICachedFilesProvider cachedFilesProvider) {
		this.remoteFileUriProvider = remoteFileUriProvider;
		this.cachedFilesProvider = cachedFilesProvider;
	}

	@Override
	public Promise<Uri> promiseFileUri(ServiceFile serviceFile) {
		return remoteFileUriProvider.promiseFileUri(serviceFile)
			.eventually(uri -> {
				if (uri == null) return Promise.empty();

				return cachedFilesProvider
					.promiseCachedFile(uri.getPath() + "?" + uri.getQuery())
					.then(cachedFile -> {
						if (cachedFile == null) return null;

						final File file = new File(cachedFile.getFileName());
						return file.exists() ? Uri.fromFile(file) : null;
					});
			});
	}
}
