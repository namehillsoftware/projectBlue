package com.lasthopesoftware.bluewater.client.browsing.files

import android.os.Build

object ServiceFileUriQueryParamsProvider : IServiceFileUriQueryParamsProvider {
	override fun getServiceFileUriQueryParams(serviceFile: ServiceFile): Array<String> = arrayOf(
		"File/GetFile",
		"File=${serviceFile.key}",
		"Quality=Medium",
		"Conversion=Android",
		"Playback=0",
		"AndroidVersion=${Build.VERSION.RELEASE}"
	)
}
