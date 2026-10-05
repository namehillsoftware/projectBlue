package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class BrowserLibrarySelection(
	private val applicationSettings: HoldApplicationSettings,
	private val applicationMessages: SendApplicationMessages
) : SelectBrowserLibrary {
    override fun selectBrowserLibrary(libraryId: LibraryId): Promise<LibraryId> =
		applicationSettings.promiseApplicationSettings().eventually { a ->
			if (a.chosenLibraryId == libraryId.id) libraryId.toPromise()
			else {
				a.chosenLibraryId = libraryId.id
				applicationSettings.promiseUpdatedSettings(a).then { s ->
					applicationMessages.sendMessage(LibraryChosenMessage(libraryId))
					LibraryId(s.chosenLibraryId)
				}
			}
		}

	class LibraryChosenMessage(val chosenLibraryId: LibraryId) : ApplicationMessage
}
