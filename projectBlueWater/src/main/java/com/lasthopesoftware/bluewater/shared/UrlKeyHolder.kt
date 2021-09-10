package com.lasthopesoftware.bluewater.shared

import java.net.URL

/**
 * Created by david on 1/17/16.
 */
class UrlKeyHolder<T>(private val url: URL, private val key: T) {
    private val  lazyHashCode = lazy {
		var calculatedHashCode = url.hashCode()
		calculatedHashCode = 31 * calculatedHashCode + (key?.hashCode() ?: 0)
		calculatedHashCode
	}

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as UrlKeyHolder<*>
        return url == that.url && key == that.key
    }

    override fun hashCode(): Int = lazyHashCode.value
}
