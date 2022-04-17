package com.lasthopesoftware.bluewater.client.browsing.items.media.files

object ServiceFileUriQueryParamsProvider : IServiceFileUriQueryParamsProvider {
	override fun getServiceFileUriQueryParams(serviceFile: ServiceFile): Array<String> = arrayOf(
		"File/GetFile",
		"File=" + serviceFile.key,
		"Quality=medium",
		"Conversion=Android",
		"Playback=0"
	)
}
