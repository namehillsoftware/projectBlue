package com.lasthopesoftware.bluewater.shared.policies.lru.GivenACache.AndTheFactoryImmediatelyHasAnError

import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenGettingTheItemFromTheCache {
	private val exception by lazy {
		val cache = LruPromiseCache<String, Any>(2)
		try {
			cache.getOrAdd("first-key") { throw Exception("Propagate me too!") }.toExpiringFuture().get()
			null
		} catch (e: ExecutionException) {
			e.cause ?: e
		}
	}

	@Test
	fun `then the exception is propagated`() {
		assertThat(exception?.message).isEqualTo("Propagate me too!")
	}
}
