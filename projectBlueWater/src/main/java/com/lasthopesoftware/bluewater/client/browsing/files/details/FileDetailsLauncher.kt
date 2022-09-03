package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity.Companion.launchFileDetailsActivity

class FileDetailsLauncher(private val context: Context) : LaunchFileDetails {
	override fun launchFileDetails(playlist: List<ServiceFile>, position: Int) =
		context.launchFileDetailsActivity(playlist, position)
}
