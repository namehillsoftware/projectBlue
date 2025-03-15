package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.access.ProvideRemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryFileProvider(private val libraryAccess: ProvideRemoteLibraryAccess) : ProvideLibraryFiles {
	override fun promiseFiles(libraryId: LibraryId, option: FileListParameters.Options, vararg params: String): Promise<List<ServiceFile>> =
		libraryAccess
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { it?.promiseFiles(option, *params).keepPromise(emptyList()) }

	override fun promiseFiles(libraryId: LibraryId): Promise<List<ServiceFile>> =
		libraryAccess
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { it?.promiseFiles().keepPromise(emptyList()) }

	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId): Promise<List<ServiceFile>> =
		libraryAccess
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { it?.promiseFiles(itemId).keepPromise(emptyList()) }

	override fun promiseFiles(libraryId: LibraryId, playlistId: PlaylistId): Promise<List<ServiceFile>> =
		libraryAccess
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { it?.promiseFiles(playlistId).keepPromise(emptyList()) }

	override fun promiseAudioFiles(libraryId: LibraryId, query: String): Promise<List<ServiceFile>> =
		libraryAccess
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { it?.promiseFiles("[Media Type]=[Audio] $query").keepPromise(emptyList()) }
}
