package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;
import com.lasthopesoftware.bluewater.client.library.items.media.audio.uri.CachedAudioFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.sync.library.items.files.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.sync.library.items.files.uri.StoredFileUriProvider;
import com.namehillsoftware.handoff.promises.Promise;

/**
 * Created by david on 7/24/15.
 * Will get the best URI for access speed.
 */
public class BestMatchUriProvider implements IFileUriProvider {
	private final Library library;
	private final StoredFileUriProvider storedFileUriProvider;
	private final CachedAudioFileUriProvider cachedAudioFileUriProvider;
	private final MediaFileUriProvider mediaFileUriProvider;
	private final RemoteFileUriProvider remoteFileUriProvider;

	public BestMatchUriProvider(Library library, StoredFileUriProvider storedFileUriProvider, CachedAudioFileUriProvider cachedAudioFileUriProvider, MediaFileUriProvider mediaFileUriProvider, RemoteFileUriProvider remoteFileUriProvider) {
		this.library = library;
		this.storedFileUriProvider = storedFileUriProvider;
		this.cachedAudioFileUriProvider = cachedAudioFileUriProvider;
		this.mediaFileUriProvider = mediaFileUriProvider;
		this.remoteFileUriProvider = remoteFileUriProvider;
	}

	@Override
	public Promise<Uri> promiseFileUri(ServiceFile serviceFile) {
		return storedFileUriProvider
			.promiseFileUri(serviceFile)
			.eventually(storedFileUri -> {
				if (storedFileUri != null)
					return new Promise<>(storedFileUri);

				if (!library.isUsingExistingFiles())
					return promiseCachedOrRemoteFileUri(serviceFile);

				return mediaFileUriProvider
					.promiseFileUri(serviceFile)
					.eventually(mediaFileUri ->
						mediaFileUri != null
							? new Promise<>(mediaFileUri)
							: promiseCachedOrRemoteFileUri(serviceFile));
			});
	}

	private Promise<Uri> promiseCachedOrRemoteFileUri(ServiceFile serviceFile) {
		return cachedAudioFileUriProvider
			.promiseFileUri(serviceFile)
			.eventually(cachedFileUri -> {
				if (cachedFileUri != null)
					return new Promise<>(cachedFileUri);

				return remoteFileUriProvider.promiseFileUri(serviceFile);
			});
	}
}
