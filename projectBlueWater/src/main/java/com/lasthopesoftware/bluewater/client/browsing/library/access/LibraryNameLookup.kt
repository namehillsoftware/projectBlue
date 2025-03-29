package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise

class LibraryNameLookup(
	private val librarySettings: ProvideLibrarySettings,
) : LookupLibraryName {
	override fun promiseLibraryName(libraryId: LibraryId): Promise<String?> =
		librarySettings
			.promiseLibrarySettings(libraryId)
			.cancelBackThen { l, _ ->
				l?.libraryName?.takeIf { it.isNotEmpty() } ?: l?.connectionSettings?.accessCode
			}
}
