package com.lasthopesoftware.bluewater.shared.android.intents

import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

interface BuildIntents {

	fun buildViewLibraryIntent(libraryId: LibraryId): Intent

	fun buildApplicationSettingsIntent(): Intent

	fun buildLibrarySettingsIntent(libraryId: LibraryId): Intent

	fun buildFileDetailsIntent(libraryId: LibraryId, playlist: Collection<ServiceFile>, position: Int): Intent

	fun buildNowPlayingIntent(): Intent

	fun buildShowDownloadsIntent(): Intent
}
