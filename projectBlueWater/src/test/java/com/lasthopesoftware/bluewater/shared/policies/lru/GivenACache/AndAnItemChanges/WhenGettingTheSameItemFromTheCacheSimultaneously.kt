package com.lasthopesoftware.bluewater.shared.policies.lru.GivenACache.AndAnItemChanges

import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheSameItemFromTheCacheSimultaneously {
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

	@Test
	fun `then the result is from the first entry`() {
		assertThat(item).isEqualTo(firstItem)
	}
}
