package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri;

import android.net.Uri;

import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

/**
 * Created by david on 7/24/15.
 */
public class RemoteFileUriProvider extends AbstractFileUriProvider {
	public RemoteFileUriProvider(IFile file) {
		super(file);
	}

	@Override
	public Uri getFileUri() {
		LoggerFactory.getLogger(RemoteFileUriProvider.class).info("Returning file URL from server.");

		final String itemUrl = getFile().getPlaybackUrl();
		if (itemUrl != null && !itemUrl.isEmpty())
			return Uri.parse(itemUrl);

		return null;
	}
}
