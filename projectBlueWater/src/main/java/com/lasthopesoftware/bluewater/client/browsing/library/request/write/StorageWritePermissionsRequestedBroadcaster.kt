package com.lasthopesoftware.bluewater.client.browsing.library.request.write

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages

class StorageWritePermissionsRequestedBroadcaster(private val sendApplicationMessages: SendApplicationMessages) :
	BroadcastWritePermissionsRequest {

	override fun sendWritePermissionsNeededBroadcast(libraryId: LibraryId) =
		sendApplicationMessages.sendMessage(WritePermissionsNeeded(libraryId))

	class WritePermissionsNeeded(val libraryId: LibraryId) : ApplicationMessage
}
