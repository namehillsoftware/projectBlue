package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile

interface LaunchFileDetails {
	fun launchFileDetails(playlist: List<ServiceFile>, position: Int)
}
