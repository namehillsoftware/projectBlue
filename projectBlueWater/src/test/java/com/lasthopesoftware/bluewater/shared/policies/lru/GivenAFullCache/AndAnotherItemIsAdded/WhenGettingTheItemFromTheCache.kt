package com.lasthopesoftware.bluewater.shared.policies.lru.GivenAFullCache.AndAnotherItemIsAdded

import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenGettingTheItemFromTheCache {
	companion object {
		private val firstItem = Any()

		private val item by lazy {
			val cache = LruPromiseCache<String, Any>(1)
			cache.getOrAdd("first-key") { Promise(firstItem) }.toExpiringFuture().get()
			cache.getOrAdd("second-key") { Promise(Any()) }.toExpiringFuture().get()
			cache.getOrAdd("first-key") { Promise(Any()) }.toExpiringFuture().get()
		}
	}

	@Test
	fun thenTheFirstItemHasChanged() {
		assertThat(item).isNotEqualTo(firstItem)
	}
}
