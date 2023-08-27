package com.lasthopesoftware.resources.uri

import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object MediaCollections {
	val ExternalAudio: Uri by lazy {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
		} else {
			MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
		}
	}
}
