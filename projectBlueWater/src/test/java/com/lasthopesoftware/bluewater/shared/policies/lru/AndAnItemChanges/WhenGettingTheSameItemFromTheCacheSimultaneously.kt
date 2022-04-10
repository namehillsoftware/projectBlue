package com.lasthopesoftware.bluewater.shared.policies.lru.AndAnItemChanges

import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenGettingTheSameItemFromTheCacheSimultaneously {
	companion object {
		private val firstItem = Any()

		private val item by lazy {
			val cache = LruPromiseCache<String, Any>(2)
			val firstPromise = DeferredPromise(firstItem)
			val secondPromise = DeferredPromise(Any())

			cache.getOrAdd("second-key") { firstPromise }

			val futureValue = cache.getOrAdd("second-key") { secondPromise }.toExpiringFuture()

			secondPromise.resolve()
			firstPromise.resolve()

			futureValue.get()
		}
	}

	@Test
	fun thenTheResultIsFromTheFirstEntry() {
		assertThat(item).isEqualTo(firstItem)
	}
}
