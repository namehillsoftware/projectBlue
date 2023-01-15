package com.lasthopesoftware.bluewater.shared.policies.lru.GivenACache.AndAddingAnItemErrors.AndItSucceedsASecondTime

import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheItemFromTheCache {
	private val expectedResult = Any()

	private val item by lazy {
		val cache = LruPromiseCache<String, Any>(2)
		try {
			cache.getOrAdd("first-key") { Promise(Exception("Oh no!")) }.toExpiringFuture().get()
		} catch (e: Throwable) {
			// ignore
		}
		cache.getOrAdd("first-key") { Promise(expectedResult) }.toExpiringFuture().get()
	}

	@Test
	fun `then the result is correct`() {
		assertThat(item).isEqualTo(expectedResult)
	}
}
