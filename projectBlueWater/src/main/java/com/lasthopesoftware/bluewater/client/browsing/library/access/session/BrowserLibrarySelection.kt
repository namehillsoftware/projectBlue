package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.namehillsoftware.handoff.promises.Promise

class BrowserLibrarySelection(
	private val applicationSettings: HoldApplicationSettings,
	private val applicationMessages: SendApplicationMessages,
	private val libraryProvider: ILibraryProvider
) : SelectBrowserLibrary {
    override fun selectBrowserLibrary(libraryId: LibraryId): Promise<Library> =
		applicationSettings.promiseApplicationSettings().eventually { a ->
			if (a.chosenLibraryId == libraryId.id) libraryProvider.promiseLibrary(libraryId)
			else {
				a.chosenLibraryId = libraryId.id
				applicationSettings.promiseUpdatedSettings(a).eventually {
					applicationMessages.sendMessage(LibraryChosenMessage(libraryId))
					libraryProvider.promiseLibrary(libraryId)
				}
			}
		}

	class LibraryChosenMessage(val chosenLibraryId: LibraryId) : ApplicationMessage
}
