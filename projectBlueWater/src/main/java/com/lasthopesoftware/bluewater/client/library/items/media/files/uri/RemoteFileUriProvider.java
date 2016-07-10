package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import org.slf4j.LoggerFactory;

/**
 * Created by david on 7/24/15.
 */
public class RemoteFileUriProvider extends AbstractFileUriProvider {
	private final ConnectionProvider connectionProvider;

	public RemoteFileUriProvider(ConnectionProvider connectionProvider, IFile file) {
		super(file);

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
