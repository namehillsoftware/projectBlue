package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.policies.caching.CachingPolicyFactory
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.namehillsoftware.handoff.promises.Promise

class CachedItemProvider(
	private val inner: ProvideItems,
	private val revisions: CheckRevisions,
	private val cachingPolicyFactory: CachingPolicyFactory,
) : ProvideItems {

	private val itemsPromise by lazy { cachingPolicyFactory.applyPolicy { id: LibraryId, itemId: KeyedIdentifier?, _: Long -> inner.promiseItems(id, itemId) } }

	override fun promiseItems(libraryId: LibraryId, itemId: KeyedIdentifier?): Promise<List<IItem>> =
		revisions
			.promiseRevision(libraryId)
			.cancelBackEventually {
				itemsPromise(libraryId, itemId, it)
			}
}
