package com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache.GivenAServiceFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.GetRawImages
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.cache.LookupImageCacheKey
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.cache.MemoryCachedImageAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheImageTwice {
	private val expectedImageBytes = byteArrayOf(18)
	private val imageBytes by lazy {
		val images = mockk<GetRawImages>()
		every { images.promiseImageBytes(LibraryId(33), ServiceFile(5555)) } returns Promise(expectedImageBytes)

		val cacheKeyLookup = mockk<LookupImageCacheKey>()
		every { cacheKeyLookup.promiseImageCacheKey(LibraryId(33), ServiceFile(5555)) } returns "the-key".toPromise()

		val memoryCachedImageAccess = MemoryCachedImageAccess(
			images,
			cacheKeyLookup,
			LruPromiseCache(1)
		)

		memoryCachedImageAccess.promiseImageBytes(LibraryId(33), ServiceFile(5555)).toExpiringFuture().get()

		every { images.promiseImageBytes(LibraryId(33), ServiceFile(5555)) } returns Promise(byteArrayOf(8))

		memoryCachedImageAccess.promiseImageBytes(LibraryId(33), ServiceFile(5555)).toExpiringFuture().get()
	}

	@Test
	fun thenTheCachedBytesAreCorrect() {
		assertThat(imageBytes).isEqualTo(expectedImageBytes)
	}
}
