package com.lasthopesoftware.resources.uri

import android.net.Uri

object PathAndQuery {
	fun Uri.pathAndQuery() = forUri(this)

    fun forUri(uri: Uri): String {
        return uri.path + "?" + uri.query
    }
}
