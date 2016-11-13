package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import org.slf4j.LoggerFactory;

/**
 * Created by david on 7/24/15.
 */
public class RemoteFileUriProvider implements IFileUriProvider {
	private final ConnectionProvider connectionProvider;

	RemoteFileUriProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Uri getFileUri(IFile file) {
		LoggerFactory.getLogger(RemoteFileUriProvider.class).info("Returning file URL from server.");

		/* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.;
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */

		final String itemUrl =
			connectionProvider
				.getUrlProvider()
				.getUrl(
					"File/GetFile",
					"File=" + Integer.toString(file.getKey()),
					"Quality=medium",
					"Conversion=Android",
					"Playback=0");

		return Uri.parse(itemUrl);
	}
}
