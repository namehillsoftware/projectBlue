package com.lasthopesoftware.resources.uri

import android.net.Uri
import java.net.URI

object PathAndQuery {
	fun Uri.pathAndQuery() = "$path?$query"
}

fun Uri.toURI(): URI = URI.create(toString())

fun URI.toUri(): Uri = Uri.parse(toString())
