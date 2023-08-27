package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

import java.net.URI

fun StoredFile.setURI(uri: URI?) = setUri(uri?.toString())
val StoredFile.URI: URI?
	get() = uri?.let(::URI)
