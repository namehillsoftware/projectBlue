package com.lasthopesoftware.bluewater.client.library.items.media.files;

public final class ServiceFileUriQueryParamsProvider implements IServiceFileUriQueryParamsProvider {

	@Override
	public String[] getServiceFileUriQueryParams(ServiceFile serviceFile) {
		return new String[]{
			"File/GetFile",
			"File=" + Integer.toString(serviceFile.getKey()),
			"Quality=medium",
			"Conversion=Android",
			"Playback=0"};
	}
}
