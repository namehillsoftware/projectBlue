package com.lasthopesoftware.bluewater.client.library.items.media.files;

import com.vedsoft.lazyj.Lazy;

/**
 * Created by david on 3/26/17.
 */

public final class ServiceFileUriQueryParamsProvider implements IServiceFileUriQueryParamsProvider {

	private static final Lazy<ServiceFileUriQueryParamsProvider> instance = new Lazy<>(ServiceFileUriQueryParamsProvider::new);

	private ServiceFileUriQueryParamsProvider() {}

	@Override
	public String[] getServiceFileUriQueryParams(ServiceFile serviceFile) {
		return new String[]{
			"File/GetFile",
			"File=" + Integer.toString(serviceFile.getKey()),
			"Quality=medium",
			"Conversion=Android",
			"Playback=0"};
	}

	public static ServiceFileUriQueryParamsProvider getInstance() {
		return instance.getObject();
	}
}
