package com.lasthopesoftware.bluewater.shared.policies.lru.GivenACache.AndAnItemIsRequested

import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCancelling {
	private val cachedPromise by lazy {
		val cache = LruPromiseCache<String, Any>(2)
		val deferredPromise = object : DeferredPromise<Any>(Any()) {
			override fun cancellationRequested() {
				resolve("cancelled")
			}
		}
		val cachedPromise = cache.getOrAdd("first-key") { deferredPromise }
		cachedPromise
	}

	private var item: Any? = null

	@BeforeAll
	fun act() {
		cachedPromise.cancel()
		item = cachedPromise.toExpiringFuture().get()
	}

	@Test
	fun `then the returned item is correct`() {
		assertThat(item).isEqualTo("cancelled")
	}
}
