package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class SelectedBrowserLibraryIdentifierProvider(private val applicationSettings: HoldApplicationSettings)
	: ProvideSelectedLibraryId, ImmediateResponse<ApplicationSettings, LibraryId?>
{
	override val selectedLibraryId: Promise<LibraryId?>
		get() = applicationSettings.promiseApplicationSettings().then(this)

	override fun respond(settings: ApplicationSettings): LibraryId? =
		settings.chosenLibraryId.takeIf { it > -1 }?.let(::LibraryId)
}
