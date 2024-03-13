package com.lasthopesoftware.bluewater.client.browsing.files.cached.disk

import android.content.Context
import android.os.Environment
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.DiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import java.io.File

class AndroidDiskCacheDirectoryProvider(private val context: Context, private val diskFileCacheConfiguration: DiskFileCacheConfiguration) : ProvideDiskCacheDirectory {
	override fun getLibraryDiskCacheDirectory(libraryId: LibraryId): File? {
		val cacheDir = File(
			getDiskCacheDir(context, diskFileCacheConfiguration.cacheName),
			libraryId.id.toString()
		)
		return if (cacheDir.exists() || cacheDir.mkdirs()) cacheDir else null
	}

	override fun getRootDiskCacheDirectory(): File? {
		val cacheDir = getDiskCacheDir(context, diskFileCacheConfiguration.cacheName)
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
