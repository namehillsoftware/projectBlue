package com.lasthopesoftware.bluewater.shared.android.intents

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import com.lasthopesoftware.bluewater.client.browsing.BrowserActivity
import com.lasthopesoftware.bluewater.client.browsing.destinationProperty
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.*
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.shared.android.makePendingIntentImmutable

private const val nowPlayingPendingIntentRequestCode = 52

class IntentBuilder(private val context: Context) : BuildIntents {

	override fun buildViewLibraryIntent(libraryId: LibraryId) = getBrowserActivityIntent(LibraryScreen(libraryId))

	override fun buildApplicationSettingsIntent() = getBrowserActivityIntent(ApplicationSettingsScreen)

	override fun buildLibrarySettingsIntent(libraryId: LibraryId) = getBrowserActivityIntent(ConnectionSettingsScreen(libraryId))

	override fun buildFileDetailsIntent(libraryId: LibraryId, playlist: Collection<ServiceFile>, position: Int) = context.getIntent<FileDetailsActivity>().apply {
		putExtra(FileDetailsActivity.libraryIdKey, libraryId)
		putExtra(FileDetailsActivity.playlistPosition, position)
		putExtra(FileDetailsActivity.playlist, playlist.map { it.key }.toIntArray())
	}

	override fun buildNowPlayingIntent() = context.getIntent<NowPlayingActivity>().apply {
		flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
	}

	override fun buildPendingNowPlayingIntent(): PendingIntent {
		val intent = buildNowPlayingIntent()
		val taskStackBuilder = TaskStackBuilder.create(context)
		taskStackBuilder.addNextIntentWithParentStack(intent)

		// Give pending intents request codes unique to their intended destination
		return taskStackBuilder.getPendingIntent(nowPlayingPendingIntentRequestCode, 0.makePendingIntentImmutable())!!
	}

	override fun buildShowDownloadsIntent(): Intent = getBrowserActivityIntent(ActiveLibraryDownloadsScreen)

	private fun getBrowserActivityIntent(destination: Destination) = context.getIntent<BrowserActivity>().apply {
		flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
		putExtra(destinationProperty, destination)
	}
}
