package com.lasthopesoftware.bluewater.client.playback.caching

import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import com.google.android.exoplayer2.upstream.cache.ContentMetadata
import com.google.android.exoplayer2.upstream.cache.ContentMetadataMutations
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.ICache
import com.lasthopesoftware.bluewater.shared.promises.toFuture
import java.io.File
import java.io.IOException
import java.util.*

class ExoPlayerCache(private val cache: ICache) : Cache {
	private val uId = 183120112753L

	companion object {
		fun cacheKey(uId: Long, key: String, position: Long, length: Long) = "$uId:$key:$position:$length"
	}

	override fun getUid(): Long = uId

	override fun release() {
		TODO("Not yet implemented")
	}

	override fun addListener(key: String, listener: Cache.Listener): NavigableSet<CacheSpan> {
		TODO("Not yet implemented")
	}

	override fun removeListener(key: String, listener: Cache.Listener) {
		TODO("Not yet implemented")
	}

	override fun getCachedSpans(key: String): NavigableSet<CacheSpan> {
		TODO("Not yet implemented")
	}

	override fun getKeys(): MutableSet<String> {
		TODO("Not yet implemented")
	}

	override fun getCacheSpace(): Long {
		TODO("Not yet implemented")
	}

	override fun startReadWrite(key: String, position: Long, length: Long): CacheSpan =
		startReadWriteNonBlocking(key, position, length) ?: CacheSpan(key, position, length)

	override fun startReadWriteNonBlocking(key: String, position: Long, length: Long): CacheSpan? {
		val cachedFile = cache.promiseCachedFile(cacheKey(uId, key, position, length)).toFuture().get()
		return cachedFile?.let { CacheSpan(key, position, length, it.lastModified(), it) }
	}

	override fun startFile(key: String, position: Long, length: Long): File =
		cache.put(cacheKey(uId, key, position, length), ByteArray(length.toInt()))
			.eventually { cache.promiseCachedFile(key) }
			.toFuture()
			.get() ?: throw IOException("File could not be created")

	override fun commitFile(file: File, length: Long) {
		TODO("Not yet implemented")
	}

	override fun releaseHoleSpan(holeSpan: CacheSpan) {
		TODO("Not yet implemented")
	}

	override fun removeResource(key: String) {
		TODO("Not yet implemented")
	}

	override fun removeSpan(span: CacheSpan) {
		TODO("Not yet implemented")
	}

	override fun isCached(key: String, position: Long, length: Long): Boolean {
		TODO("Not yet implemented")
	}

	override fun getCachedLength(key: String, position: Long, length: Long): Long {
		TODO("Not yet implemented")
	}

	override fun getCachedBytes(key: String, position: Long, length: Long): Long {
		TODO("Not yet implemented")
	}

	override fun applyContentMetadataMutations(key: String, mutations: ContentMetadataMutations) {
		TODO("Not yet implemented")
	}

	override fun getContentMetadata(key: String): ContentMetadata {
		TODO("Not yet implemented")
	}

	private data class CacheKey(val uId: Long, val key: String, val position: Long, val length: Long) {
		override fun toString(): String = "$uId:$key:$position:$length"
	}
}
