package com.lasthopesoftware.bluewater.client.browsing.library.request.read

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages

class StorageReadPermissionsRequestedBroadcaster(private val sendApplicationMessages: SendApplicationMessages) :
	BroadcastReadPermissionsRequest {

	class ReadPermissionsNeeded(val libraryId: LibraryId) : ApplicationMessage

	override fun sendReadPermissionsRequestedBroadcast(libraryId: LibraryId) =
		sendApplicationMessages.sendMessage(ReadPermissionsNeeded(libraryId))
}
