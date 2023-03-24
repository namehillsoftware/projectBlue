package com.lasthopesoftware.bluewater.client.settings

import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

interface BuildIntents {
	fun buildLibrarySettingsIntent(libraryId: LibraryId): Intent

	fun buildNewLibraryIntent(): Intent

	fun buildFileDetailsIntent(playlist: Collection<ServiceFile>, position: Int): Intent

	fun buildNowPlayingIntent(): Intent
}
