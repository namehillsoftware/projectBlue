package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri.StoredFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

/**
 * Created by david on 7/24/15.
 * Will get the best URI for access speed.
 */
public class BestMatchUriProvider implements IFileUriProvider {
	private final Library library;
	private final StoredFileUriProvider storedFileUriProvider;
	private final MediaFileUriProvider mediaFileUriProvider;
	private final RemoteFileUriProvider remoteFileUriProvider;

	public BestMatchUriProvider(Library library, StoredFileUriProvider storedFileUriProvider, MediaFileUriProvider mediaFileUriProvider, RemoteFileUriProvider remoteFileUriProvider) {
		this.library = library;
		this.storedFileUriProvider = storedFileUriProvider;
		this.mediaFileUriProvider = mediaFileUriProvider;
		this.remoteFileUriProvider = remoteFileUriProvider;
	}

	@Override
	public Promise<Uri> getFileUri(ServiceFile serviceFile) {
		return
			storedFileUriProvider
				.getFileUri(serviceFile)
				.eventually(storedFileUri -> {
					if (storedFileUri != null)
						return new Promise<>(storedFileUri);

					if (!library.isUsingExistingFiles())
						return remoteFileUriProvider.getFileUri(serviceFile);

					return
						mediaFileUriProvider
							.getFileUri(serviceFile)
							.eventually(mediaFileUri -> {
								if (mediaFileUri != null)
									return new Promise<>(mediaFileUri);

								return remoteFileUriProvider.getFileUri(serviceFile);
							});
				});
	}
}
