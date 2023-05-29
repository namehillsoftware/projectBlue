package com.lasthopesoftware.bluewater.client.browsing.files.cached.disk

import android.content.Context
import android.os.Environment
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.IDiskFileCacheConfiguration
import java.io.File

class AndroidDiskCacheDirectoryProvider(private val context: Context) : IDiskCacheDirectoryProvider {
	override fun getDiskCacheDirectory(diskFileCacheConfiguration: IDiskFileCacheConfiguration): File? {
		val cacheDir = File(
			getDiskCacheDir(context, diskFileCacheConfiguration.cacheName),
			diskFileCacheConfiguration.library.id.toString()
		)
		return if (cacheDir.exists() || cacheDir.mkdirs()) cacheDir else null
	}

	companion object {
		private fun getDiskCacheDir(context: Context, uniqueName: String): File {
			// Check if media is mounted or storage is built-in, if so, try and use external cache dir
			// otherwise use internal cache dir
			val cacheDir =
				if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) context.externalCacheDir else context.cacheDir
			return File(cacheDir, uniqueName)
		}
	}
}
