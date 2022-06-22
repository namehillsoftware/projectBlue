package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.FileDetailsActivity.Companion.launchFileDetailsActivity

class FileDetailsLauncher(private val context: Context) : LaunchFileDetails {
	override fun launchFileDetails(serviceFile: ServiceFile) =
		context.launchFileDetailsActivity(serviceFile)
}
