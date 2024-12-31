package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.PlaybackLibraryItems
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.PollForLibraryConnections
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.LookupImageCacheKey

interface LibraryConnectionDependencies {
	val urlKeyProvider: UrlKeyProvider
	val revisionProvider: LibraryRevisionProvider
	val filePropertiesStorage: FilePropertyStorage
	val itemProvider: ProvideItems
	val itemFileProvider: ProvideItemFiles
	val libraryFilesProvider: LibraryFileProvider
	val playbackLibraryItems: PlaybackLibraryItems
	val pollForConnections: PollForLibraryConnections
	val imageBytesProvider: GetImageBytes
	val imageCacheKeyLookup: LookupImageCacheKey
	val libraryFilePropertiesProvider: CachedFilePropertiesProvider
	val freshLibraryFileProperties: ProvideFreshLibraryFileProperties
	val connectionAuthenticationChecker: ConnectionAuthenticationChecker
    val itemStringListProvider: ProvideFileStringListForItem
}
