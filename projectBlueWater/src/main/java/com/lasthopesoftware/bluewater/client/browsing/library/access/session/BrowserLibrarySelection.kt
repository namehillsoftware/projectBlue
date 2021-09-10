package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages
import com.namehillsoftware.handoff.promises.Promise

class BrowserLibrarySelection(
    private val applicationSettings: HoldApplicationSettings,
    private val localBroadcastManager: SendMessages,
    private val libraryProvider: ILibraryProvider
) : SelectBrowserLibrary {
    override fun selectBrowserLibrary(libraryId: LibraryId): Promise<Library> =
		applicationSettings.promiseApplicationSettings().eventually { a ->
			if (a.chosenLibraryId == libraryId.id) libraryProvider.getLibrary(libraryId)
			else {
				a.chosenLibraryId = libraryId.id
				applicationSettings.promiseUpdatedSettings(a).eventually {
					val broadcastIntent = Intent(libraryChosenEvent)
					broadcastIntent.putExtra(chosenLibraryId, libraryId.id)
					localBroadcastManager.sendBroadcast(broadcastIntent)
					libraryProvider.getLibrary(libraryId)
				}
			}
		}

    companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(BrowserLibrarySelection::class.java) }

		val libraryChosenEvent by lazy { magicPropertyBuilder.buildProperty("libraryChosenEvent") }
		val chosenLibraryId by lazy { magicPropertyBuilder.buildProperty("chosenLibraryId") }
    }
}
