package com.lasthopesoftware.bluewater.android.intents

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.client.EntryActivity
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.ActiveLibraryDownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ApplicationSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SearchScreen
import com.lasthopesoftware.bluewater.client.destinationProperty
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService

class IntentBuilder(private val context: Context) : BuildIntents {

	override fun buildViewLibraryIntent(libraryId: LibraryId): Intent = getBrowserActivityIntent(LibraryScreen(libraryId))

	override fun buildLibrarySearchIntent(libraryId: LibraryId, filePropertyFilter: FileProperty): Intent = getBrowserActivityIntent(SearchScreen(libraryId, filePropertyFilter))

	override fun buildApplicationSettingsIntent(): Intent = getBrowserActivityIntent(ApplicationSettingsScreen)

	override fun buildLibrarySettingsIntent(libraryId: LibraryId): Intent = getBrowserActivityIntent(ConnectionSettingsScreen(libraryId))
	override fun buildLibraryServerSettingsPendingIntent(libraryId: LibraryId): PendingIntent {
		val baseIntent = buildLibrarySettingsIntent(libraryId)
		return PendingIntent.getActivity(context, 0, baseIntent, 0.makePendingIntentImmutable())
	}

	@OptIn(UnstableApi::class)
	override fun buildFileDetailsIntent(libraryId: LibraryId, item: IItem, positionedFile: PositionedFile) = context.getIntent<FileDetailsActivity>().apply {
		putExtra(FileDetailsActivity.libraryIdKey, libraryId)
		putExtra(FileDetailsActivity.itemId, item.itemId)
		putExtra(FileDetailsActivity.positionedFile, positionedFile)
	}

	@OptIn(UnstableApi::class)
	override fun buildFileDetailsIntent(libraryId: LibraryId, playlist: Collection<ServiceFile>, position: Int) = context.getIntent<FileDetailsActivity>().apply {
		putExtra(FileDetailsActivity.libraryIdKey, libraryId)
		putExtra(FileDetailsActivity.playlistPosition, position)
		putExtra(FileDetailsActivity.playlist, playlist.map { it.key }.toTypedArray())
	}

	override fun buildNowPlayingIntent(libraryId: LibraryId): Intent = getBrowserActivityIntent(NowPlayingScreen(libraryId))

	override fun buildPendingNowPlayingIntent(libraryId: LibraryId): PendingIntent {
		val intent = buildNowPlayingIntent(libraryId)
		return PendingIntent.getActivity(context, 0, intent, 0.makePendingIntentImmutable())
	}

	@OptIn(UnstableApi::class)
	override fun buildPendingPausePlaybackIntent(): PendingIntent = PlaybackService.pendingPauseIntent(context)

	override fun buildPendingShowDownloadsIntent(): PendingIntent {
		val baseIntent = buildShowDownloadsIntent()
		return PendingIntent.getActivity(context, 0, baseIntent, 0.makePendingIntentImmutable())
	}

	private fun buildShowDownloadsIntent(): Intent = getBrowserActivityIntent(ActiveLibraryDownloadsScreen)

	private fun getBrowserActivityIntent(destination: Destination): Intent = context.getIntent<EntryActivity>().apply {
		flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
		// Set action to uniquely identify intents when compared with `filterEquals`, as the extras are not enough.
		data = "destination://${destination.javaClass.name}".toUri()
		putExtra(destinationProperty, destination)
	}
}
