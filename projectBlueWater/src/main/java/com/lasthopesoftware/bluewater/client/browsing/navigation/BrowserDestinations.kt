package com.lasthopesoftware.bluewater.client.browsing.navigation

import android.os.Parcelable
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import kotlinx.parcelize.Parcelize

sealed interface Destination : Parcelable

@Parcelize
data object ApplicationSettingsScreen : Destination

@Parcelize
data object NewConnectionSettingsScreen : Destination

@Parcelize
data object HiddenSettingsScreen : Destination

sealed interface ConnectingDestination : Destination

@Parcelize
data object ActiveLibraryDownloadsScreen : ConnectingDestination

@Parcelize
data object SelectedLibraryReRouter : ConnectingDestination

sealed interface LibraryDestination : Destination {
	val libraryId: LibraryId
}

@Parcelize
class ConnectionSettingsScreen(override val libraryId: LibraryId) : LibraryDestination

@Parcelize
data class NowPlayingScreen(override val libraryId: LibraryId) : LibraryDestination

@Parcelize
class BrowsedFileDetailsScreen(
	override val libraryId: LibraryId,
	val item: IItem?,
	val positionedFile: PositionedFile,
) : LibraryDestination

@Parcelize
class SearchedFileDetailsScreen(
	override val libraryId: LibraryId,
	val query: String,
	val positionedFile: PositionedFile,
) : LibraryDestination

@Parcelize
class FileDetailsFromNowPlayingScreen(
	override val libraryId: LibraryId,
	val positionedFile: PositionedFile,
) : LibraryDestination

sealed interface BrowserLibraryDestination : LibraryDestination

@Parcelize
class LibraryScreen(override val libraryId: LibraryId) : BrowserLibraryDestination

@Parcelize
data class ItemScreen(override val libraryId: LibraryId, val item: IItem) : BrowserLibraryDestination

@Parcelize
class DownloadsScreen(override val libraryId: LibraryId) : BrowserLibraryDestination

@Parcelize
class SearchScreen(override val libraryId: LibraryId, val filePropertyFilter: FileProperty? = null) : BrowserLibraryDestination
