package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

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
	public Uri getFileUri() {
		LoggerFactory.getLogger(RemoteFileUriProvider.class).info("Returning file URL from server.");

		final String itemUrl = getFile().getPlaybackUrl(connectionProvider);
		if (itemUrl != null && !itemUrl.isEmpty())
			return Uri.parse(itemUrl);

		return null;
	}
}
