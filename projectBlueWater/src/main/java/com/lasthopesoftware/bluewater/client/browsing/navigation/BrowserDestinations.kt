package com.lasthopesoftware.bluewater.client.browsing.navigation

import android.os.Parcelable
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import kotlinx.parcelize.Parcelize

sealed interface Destination : Parcelable

@Parcelize
object ApplicationSettingsScreen : Destination

@Parcelize
object AboutScreen : Destination

@Parcelize
object NewConnectionSettingsScreen : Destination

@Parcelize
object HiddenSettingsScreen : Destination

sealed interface ConnectingDestination : Destination

@Parcelize
object ActiveLibraryDownloadsScreen : ConnectingDestination

@Parcelize
object SelectedLibraryReRouter : ConnectingDestination

sealed interface LibraryDestination : Destination {
	val libraryId: LibraryId
}

@Parcelize
class LibraryScreen(override val libraryId: LibraryId) : LibraryDestination

@Parcelize
class ItemScreen(override val libraryId: LibraryId, val item: Item) : LibraryDestination

@Parcelize
class DownloadsScreen(override val libraryId: LibraryId) : LibraryDestination

@Parcelize
class ConnectionSettingsScreen(override val libraryId: LibraryId) : LibraryDestination

@Parcelize
class SearchScreen(override val libraryId: LibraryId) : LibraryDestination