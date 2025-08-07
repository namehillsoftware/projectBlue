package com.lasthopesoftware.bluewater.android.intents

import android.app.PendingIntent
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

interface BuildIntents {

	fun buildViewLibraryIntent(libraryId: LibraryId): Intent

	fun buildLibrarySearchIntent(libraryId: LibraryId, filePropertyFilter: FileProperty): Intent

	fun buildApplicationSettingsIntent(): Intent

	fun buildLibrarySettingsIntent(libraryId: LibraryId): Intent

	fun buildLibraryServerSettingsPendingIntent(libraryId: LibraryId): PendingIntent

	fun buildFileDetailsIntent(libraryId: LibraryId, item: IItem, positionedFile: PositionedFile): Intent

	fun buildFileDetailsIntent(libraryId: LibraryId, playlist: Collection<ServiceFile>, position: Int): Intent

	fun buildNowPlayingIntent(libraryId: LibraryId): Intent

	fun buildPendingNowPlayingIntent(libraryId: LibraryId): PendingIntent

	fun buildPendingPausePlaybackIntent(): PendingIntent

	fun buildPendingShowDownloadsIntent(): PendingIntent
}
