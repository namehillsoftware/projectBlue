package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import android.content.ContentValues
import android.net.Uri

interface ExternalContent {
	val collection: Uri
	val type: String
	fun toContentValues(): ContentValues
}
