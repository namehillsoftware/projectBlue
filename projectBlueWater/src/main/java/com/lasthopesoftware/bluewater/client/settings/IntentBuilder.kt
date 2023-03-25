package com.lasthopesoftware.bluewater.client.settings

import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.shared.android.intents.getIntent

class IntentBuilder(private val context: Context) : BuildIntents {

	override fun buildLibrarySettingsIntent(libraryId: LibraryId): Intent {
		val returnIntent = context.getIntent<EditClientSettingsActivity>()
		returnIntent.putExtra(EditClientSettingsActivity.serverIdExtra, libraryId.id)
		return returnIntent
	}

	override fun buildNewLibraryIntent(): Intent {
		val returnIntent = context.getIntent<EditClientSettingsActivity>()
		returnIntent.putExtra(EditClientSettingsActivity.serverIdExtra, -1)
		return returnIntent
	}

	override fun buildFileDetailsIntent(playlist: Collection<ServiceFile>, position: Int) = context.getIntent<FileDetailsActivity>().apply {
		putExtra(FileDetailsActivity.playlistPosition, position)
		putExtra(FileDetailsActivity.playlist, playlist.map { it.key }.toIntArray())
	}

	override fun buildNowPlayingIntent(): Intent {
		val viewIntent = context.getIntent<NowPlayingActivity>()
		viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
		return viewIntent
	}
}
