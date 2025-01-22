package com.lasthopesoftware.resources.bitmaps

import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.lasthopesoftware.policies.retries.RetryOnRejectionLazyPromise
import com.namehillsoftware.handoff.promises.Promise

class DefaultAwareCachingBitmapProducer(
	private val inner: ProduceBitmaps,
	private val defaultImageProvider: ProvideDefaultImage,
	private val cache: CachePromiseFunctions<Int, Bitmap?> = companionCache,
) : ProduceBitmaps {
	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 1

		private val companionCache by lazy { LruPromiseCache<Int, Bitmap?>(MAX_MEMORY_CACHE_SIZE) }
	}

	private val promisedDefaultImageBitmap by RetryOnRejectionLazyPromise {
		defaultImageProvider
			.promiseImageBytes()
			.then { bytes ->
				Pair(bytes.hashCode(), RetryOnRejectionLazyPromise { inner.promiseBitmap(bytes) })
			}
	}

	override fun promiseBitmap(byteArray: ByteArray): Promise<Bitmap?> =
		promisedDefaultImageBitmap
			.eventually { (defaultHash, defaultBmp) ->
				val hash = byteArray.hashCode()
				if (hash == defaultHash) defaultBmp.value
				else cache.getOrAdd(hash) { inner.promiseBitmap(byteArray) }
			}
}
