package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.policies.caching.CachingPolicyFactory
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.namehillsoftware.handoff.promises.Promise

class RevisionCachedLibraryFileProvider(
	inner: ProvideLibraryFiles,
	private val revisionProvider: CheckRevisions,
	cachingPolicy: CachingPolicyFactory
) : ProvideLibraryFiles {
	private val promiseLibraryFiles by lazy {
		cachingPolicy.applyPolicy { id: LibraryId, _: Long -> inner.promiseFiles(id) }
	}

	private val promiseItemFiles by lazy {
		cachingPolicy.applyPolicy { l: LibraryId, i: ItemId, _: Long -> inner.promiseFiles(l, i) }
	}

	private val promisePlaylistFiles by lazy {
		cachingPolicy.applyPolicy { l: LibraryId, p: PlaylistId, _: Long -> inner.promiseFiles(l, p) }
	}

	private val promiseAudioFilesPolicy by lazy {
		cachingPolicy.applyPolicy { l: LibraryId, q: String, _: Long -> inner.promiseAudioFiles(l, q) }
	}

	override fun promiseFiles(libraryId: LibraryId): Promise<List<ServiceFile>> =
		revisionProvider
			.promiseRevision(libraryId)
			.cancelBackEventually { r -> promiseLibraryFiles(libraryId, r) }

	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId): Promise<List<ServiceFile>> =
		revisionProvider
			.promiseRevision(libraryId)
			.cancelBackEventually { r -> promiseItemFiles(libraryId, itemId, r) }

	override fun promiseFiles(libraryId: LibraryId, playlistId: PlaylistId): Promise<List<ServiceFile>> =
		revisionProvider
			.promiseRevision(libraryId)
			.cancelBackEventually { r -> promisePlaylistFiles(libraryId, playlistId, r) }

	override fun promiseAudioFiles(libraryId: LibraryId, query: String): Promise<List<ServiceFile>> =
		revisionProvider
			.promiseRevision(libraryId)
			.cancelBackEventually { r -> promiseAudioFilesPolicy(libraryId, query, r) }
}
