package com.lasthopesoftware.bluewater.client.browsing.library.request.read

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder.Companion.buildMagicPropertyName
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages

class StorageReadPermissionsRequestedBroadcaster(private val sendApplicationMessages: SendApplicationMessages) :
	BroadcastReadPermissionsRequest {

	companion object {
		val readPermissionsLibraryId by lazy { buildMagicPropertyName<StorageReadPermissionsRequestedBroadcaster>("readPermissionsLibraryId") }
	}

	class ReadPermissionsNeeded(val libraryId: LibraryId) : ApplicationMessage

	override fun sendReadPermissionsRequestedBroadcast(libraryId: LibraryId) {
		sendApplicationMessages.sendMessage(ReadPermissionsNeeded(libraryId))
	}
}
