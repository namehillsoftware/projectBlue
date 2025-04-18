package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.policies.ApplyExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

class DelegatingLibraryFileProvider(inner: ProvideLibraryFiles, policies: ApplyExecutionPolicies) : ProvideLibraryFiles {
	private val promiseLibraryFiles by lazy { policies.applyPolicy<LibraryId, List<ServiceFile>>(inner::promiseFiles) }
	private val promiseItemFiles by lazy { policies.applyPolicy<LibraryId, ItemId, List<ServiceFile>>(inner::promiseFiles) }
	private val promisePlaylistFiles by lazy { policies.applyPolicy<LibraryId, PlaylistId, List<ServiceFile>>(inner::promiseFiles) }
	private val promiseAudioFilesPolicy by lazy { policies.applyPolicy<LibraryId, String, List<ServiceFile>>(inner::promiseAudioFiles) }

	override fun promiseFiles(libraryId: LibraryId): Promise<List<ServiceFile>> = promiseLibraryFiles(libraryId)

	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId): Promise<List<ServiceFile>> =
		promiseItemFiles(libraryId, itemId)

	override fun promiseFiles(libraryId: LibraryId, playlistId: PlaylistId): Promise<List<ServiceFile>> =
		promisePlaylistFiles(libraryId, playlistId)

	override fun promiseAudioFiles(libraryId: LibraryId, query: String): Promise<List<ServiceFile>> =
		promiseAudioFilesPolicy(libraryId, query)
}
