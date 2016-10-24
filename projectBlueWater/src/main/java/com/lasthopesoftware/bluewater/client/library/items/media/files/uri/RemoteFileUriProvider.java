package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import org.slf4j.LoggerFactory;

/**
 * Created by david on 7/24/15.
 */
class RemoteFileUriProvider implements IFileUriProvider {
	private final ConnectionProvider connectionProvider;

	RemoteFileUriProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Uri getFileUri(IFile file) {
		LoggerFactory.getLogger(RemoteFileUriProvider.class).info("Returning file URL from server.");

		final String itemUrl = file.getPlaybackUrl(connectionProvider);
		if (itemUrl != null && !itemUrl.isEmpty())
			return Uri.parse(itemUrl);

		return null;
	}
}
