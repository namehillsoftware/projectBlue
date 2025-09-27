package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.namehillsoftware.handoff.promises.Promise

class FreshestRevisionFilePropertiesProvider(
	private val inner: ProvideFreshLibraryFileProperties,
	private val urlKeys: ProvideUrlKey,
	private val revisionChecker: CheckRevisions,
	private val cache: CachePromiseFunctions<Pair<Long, UrlKeyHolder<ServiceFile>>, Map<String, String>>,
) : ProvideFreshLibraryFileProperties {
	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> =
		urlKeys
			.promiseUrlKey(libraryId, serviceFile)
			.cancelBackEventually { urlKey ->
				urlKey
					?.let {
						revisionChecker
							.promiseRevision(libraryId)
							.cancelBackEventually { r ->
								cache.getOrAdd(Pair(r, it)) { inner.promiseFileProperties(libraryId, serviceFile) }
							}
					}
					?: inner.promiseFileProperties(libraryId, serviceFile)
			}
}
