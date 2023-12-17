package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import com.lasthopesoftware.resources.uri.MediaCollections
import java.io.File

private const val relativeRoot = "Music"

data class ExternalMusicContent(
	val displayName: String? = null,
	val artist: String? = null,
	val album: String? = null,
	val relativePath: String? = null,
) : ExternalContent {

	override val collection: Uri
		get() = MediaCollections.ExternalAudio

	override fun toContentValues(): ContentValues = ContentValues().apply {
		put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
		put(MediaStore.Audio.Media.ARTIST, artist)
		put(MediaStore.Audio.Media.ALBUM, album)
		put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath?.let { File(relativeRoot, it).path } ?: relativeRoot)
		put(MediaStore.Audio.Media.IS_PENDING, 1)
	}
}
