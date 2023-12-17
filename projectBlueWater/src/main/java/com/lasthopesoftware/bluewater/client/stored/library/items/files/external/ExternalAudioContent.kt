package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import android.content.ContentValues
import android.provider.MediaStore

data class ExternalAudioContent(
	val displayName: String? = null,
	val artist: String? = null,
	val album: String? = null,
	val relativePath: String? = null,
) : ExternalContent {
	override fun toContentValues(): ContentValues = ContentValues().apply {
		put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
		put(MediaStore.Audio.Media.ARTIST, artist)
		put(MediaStore.Audio.Media.ALBUM, album)
		put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
		put(MediaStore.Audio.Media.IS_PENDING, 1)
	}
}
