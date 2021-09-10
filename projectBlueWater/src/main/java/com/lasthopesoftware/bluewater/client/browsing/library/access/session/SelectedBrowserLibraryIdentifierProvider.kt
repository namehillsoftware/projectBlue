package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 2/12/17.
 */
class SelectedBrowserLibraryIdentifierProvider(private val applicationSettings: HoldApplicationSettings) : ProvideSelectedLibraryId {
	override val selectedLibraryId: Promise<LibraryId?>
		get() = applicationSettings.promiseApplicationSettings()
				.then { s -> s.chosenLibraryId.takeIf { it > -1 }?.let(::LibraryId) }
}
