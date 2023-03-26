package com.lasthopesoftware.bluewater.shared.android.intents

import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.items.*
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivity

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

	override fun buildShowDownloadsIntent(): Intent =
		context.getIntent<ItemBrowserActivity>().apply {
			action = "Downloads"
		}

	override fun buildItemBrowserIntent(libraryId: LibraryId): Intent =
		context.getIntent<ItemBrowserActivity>().apply {
			putExtra(libraryIdProperty, libraryId.id)
		}

	override fun buildItemBrowserIntent(libraryId: LibraryId, item: Item): Intent =
		buildItemBrowserIntent(libraryId, item).apply {
			item.playlistId?.also { putExtra(playlistIdProperty, it.id) }
		}

	override fun buildItemBrowserIntent(libraryId: LibraryId, item: IItem): Intent =
		buildItemBrowserIntent(libraryId).apply {
			putExtra(keyProperty, item.key)
			putExtra(itemTitleProperty, item.value)
		}
}
