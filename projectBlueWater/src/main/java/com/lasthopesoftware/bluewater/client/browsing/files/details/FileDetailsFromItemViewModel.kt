package com.lasthopesoftware.bluewater.client.browsing.files.details

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.list.PlaybackLibraryItems
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.namehillsoftware.handoff.promises.Promise

class FileDetailsFromItemViewModel(
	private val playbackLibraryItems: PlaybackLibraryItems,
	private val loadFileDetailsState: LoadFileDetailsState,
	private val fileDetailsState: FileDetailsState,
) : ViewModel(), PlayableFileDetailsState, FileDetailsState by fileDetailsState  {
	private var activeKeyedId: KeyedIdentifier? = null
	private var activePositionedFile: PositionedFile? = null

	fun load(libraryId: LibraryId, itemId: KeyedIdentifier, positionedFile: PositionedFile): Promise<Unit> {
		activeKeyedId = itemId
		activePositionedFile = positionedFile
		return loadFileDetailsState.load(libraryId, positionedFile.serviceFile)
	}

	override fun play() {
		val libraryId = activeLibraryId ?: return
		val positionedFile = activePositionedFile ?: return
		when (val id = activeKeyedId) {
			is ItemId -> playbackLibraryItems.playItem(libraryId, id, positionedFile.playlistPosition)
			is PlaylistId -> playbackLibraryItems.playPlaylist(libraryId, id, positionedFile.playlistPosition)
		}
	}
}
