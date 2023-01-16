package com.lasthopesoftware.bluewater.shared.policies.lru.GivenACache.AndAnItemIsRequestedByTwoCallers

import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenTheSecondCallerCancels {
	private val cachedItem = Any()

	private val mut by lazy {
		val cache = LruPromiseCache<String, Any>(2)
		val deferredPromise = object : DeferredPromise<Any>(cachedItem) {
			override fun run() {
				resolve("cancelled")
			}
		}
		cache.getOrAdd("first-key") { deferredPromise }
		val secondCachedPromise = cache.getOrAdd("first-key") {
			object : DeferredPromise<Any>(Any()) {
				override fun run() {
					resolve("cancelled-2")
				}
			}
		}

		Pair(deferredPromise, secondCachedPromise)
	}

	private var item: Any? = null

	@BeforeAll
	fun act() {
		val (deferredPromise, secondCachedPromise) = mut
		secondCachedPromise.cancel()
		deferredPromise.resolve()
		item = secondCachedPromise.toExpiringFuture().get()
	}

	@Test
	fun `then the returned item is correct`() {
		assertThat(item).isEqualTo(cachedItem)
	}
}
