package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import android.content.ContentValues

interface ExternalContent {
	fun toContentValues(): ContentValues
}
