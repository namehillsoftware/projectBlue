package com.lasthopesoftware.bluewater.client.browsing.files.cached.disk

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import java.io.File

interface IDiskCacheDirectoryProvider {
    fun getLibraryDiskCacheDirectory(libraryId: LibraryId): File?
    fun getRootDiskCacheDirectory(): File?
}
