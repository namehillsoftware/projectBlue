package com.lasthopesoftware.bluewater.client.browsing.files.access

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.shared.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.namehillsoftware.handoff.promises.Promise

class CachedItemFileProvider(
	private val inner: ProvideItemFiles,
	private val revisions: CheckRevisions,
	private val itemFunctionCache: CachePromiseFunctions<Pair<Triple<LibraryId, ItemId?, FileListParameters.Options>, Int>, List<ServiceFile>>,
) : ProvideItemFiles {

	companion object {

		private val itemFunctionCache = LruPromiseCache<Pair<Triple<LibraryId, ItemId?, FileListParameters.Options>, Int>, List<ServiceFile>>(10)

		fun getInstance(context: Context): CachedItemFileProvider {
			val libraryConnectionProvider = context.buildNewConnectionSessionManager()

			return CachedItemFileProvider(
				ItemFileProvider(
					ItemStringListProvider(
						FileListParameters,
						LibraryFileStringListProvider(libraryConnectionProvider),
					)
				),
				LibraryRevisionProvider(libraryConnectionProvider),
				itemFunctionCache
			)
		}
	}

	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId?, options: FileListParameters.Options): Promise<List<ServiceFile>> =
		revisions
			.promiseRevision(libraryId)
			.eventually { revision ->
				itemFunctionCache.getOrAdd(Pair(Triple(libraryId, itemId, options), revision)) { inner.promiseFiles(libraryId, itemId, options) }
			}
}
