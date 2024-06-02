package com.lasthopesoftware.bluewater.client.browsing.files

import android.os.Build

object ServiceFileUriQueryParamsProvider : IServiceFileUriQueryParamsProvider {

	/* Playback:
	 * 0: Downloading (not real-time playback);
	 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.;
	 * 2: Real-time playback, no playback statistics handling (default: )
	 */
	override fun getServiceFileUriQueryParams(serviceFile: ServiceFile): Array<String> = arrayOf(
		"File/GetFile",
		"File=${serviceFile.key}",
		"Quality=Medium",
		"Conversion=Android",
		"Playback=0",
		"AndroidVersion=${Build.VERSION.RELEASE}"
	)
}
