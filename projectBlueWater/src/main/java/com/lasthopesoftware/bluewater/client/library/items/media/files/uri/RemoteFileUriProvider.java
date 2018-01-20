package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.LoggerFactory;

/**
 * Created by david on 7/24/15.
 */
public class RemoteFileUriProvider implements IFileUriProvider {
	private final IConnectionProvider connectionProvider;
	private final IServiceFileUriQueryParamsProvider serviceFileUriQueryParamsProvider;

	public RemoteFileUriProvider(IConnectionProvider connectionProvider, IServiceFileUriQueryParamsProvider serviceFileUriQueryParamsProvider) {
		this.connectionProvider = connectionProvider;
		this.serviceFileUriQueryParamsProvider = serviceFileUriQueryParamsProvider;
	}

	@Override
	public Promise<Uri> getFileUri(ServiceFile serviceFile) {
		LoggerFactory.getLogger(RemoteFileUriProvider.class).info("Returning serviceFile URL from server.");

		/* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.;
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */

		final String itemUrl =
			connectionProvider
				.getUrlProvider()
				.getUrl(serviceFileUriQueryParamsProvider.getServiceFileUriQueryParams(serviceFile));

		return new Promise<>(Uri.parse(itemUrl));
	}
}
