package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise

class CaseInsensitiveFilePropertiesProvider(
	private val inner: ProvideFreshLibraryFileProperties
) : ProvideFreshLibraryFileProperties {
	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> =
		inner
			.promiseFileProperties(libraryId, serviceFile)
			.cancelBackThen { map, _ ->
				map.toSortedMap(String.CASE_INSENSITIVE_ORDER)
			}
}
