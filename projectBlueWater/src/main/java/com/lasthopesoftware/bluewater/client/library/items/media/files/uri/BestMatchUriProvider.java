package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.content.Context;
import android.net.Uri;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri.StoredFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.Promise;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;

/**
 * Created by david on 7/24/15.
 * Will get the best URI for access speed.
 */
public class BestMatchUriProvider implements IFileUriProvider {
	private final Library library;
	private final StoredFileUriProvider storedFileUriProvider;
	private final MediaFileUriProvider mediaFileUriProvider;
	private final RemoteFileUriProvider remoteFileUriProvider;

	public BestMatchUriProvider(Context context, IConnectionProvider connectionProvider, Library library) {
		this(context, connectionProvider, library, new ExternalStorageReadPermissionsArbitratorForOs(context));
	}

	private BestMatchUriProvider(Context context, IConnectionProvider connectionProvider, Library library, IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator) {
		this.library = library;
		storedFileUriProvider = new StoredFileUriProvider(context, library, externalStorageReadPermissionsArbitrator);

		final IFilePropertiesContainerRepository filePropertiesContainerRepository = FilePropertyCache.getInstance();
		final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertiesContainerRepository, new FilePropertiesProvider(connectionProvider, filePropertiesContainerRepository));
		mediaFileUriProvider = new MediaFileUriProvider(context, new MediaQueryCursorProvider(context, cachedFilePropertiesProvider), externalStorageReadPermissionsArbitrator, library);

		remoteFileUriProvider = new RemoteFileUriProvider(connectionProvider, ServiceFileUriQueryParamsProvider.getInstance());
	}

	@Override
	public Promise<Uri> getFileUri(ServiceFile serviceFile) {
		return
			storedFileUriProvider
				.getFileUri(serviceFile)
				.thenPromise(storedFileUri -> {
					if (storedFileUri != null)
						return new Promise<>(storedFileUri);

					if (!library.isUsingExistingFiles())
						return remoteFileUriProvider.getFileUri(serviceFile);

					return
						mediaFileUriProvider
							.getFileUri(serviceFile)
							.thenPromise(mediaFileUri -> {
								if (mediaFileUri != null)
									return new Promise<>(mediaFileUri);

								return remoteFileUriProvider.getFileUri(serviceFile);
							});
				});
	}
}
