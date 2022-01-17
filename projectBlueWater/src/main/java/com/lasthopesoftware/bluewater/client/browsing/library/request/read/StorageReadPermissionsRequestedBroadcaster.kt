package com.lasthopesoftware.bluewater.client.browsing.library.request.read

import android.content.Intent
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder.Companion.buildMagicPropertyName
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

/**
 * Created by david on 7/3/16.
 */
class StorageReadPermissionsRequestedBroadcaster(private val sendMessages: SendMessages) : IStorageReadPermissionsRequestedBroadcast {

	companion object {
		val readPermissionsNeeded by lazy { buildMagicPropertyName<StorageReadPermissionsRequestedBroadcaster>("readPermissionsNeeded") }
		val readPermissionsLibraryId by lazy { buildMagicPropertyName<StorageReadPermissionsRequestedBroadcaster>("readPermissionsLibraryId") }
	}

	override fun sendReadPermissionsRequestedBroadcast(libraryId: Int) {
		val readPermissionsNeededIntent = Intent(readPermissionsNeeded)
		readPermissionsNeededIntent.putExtra(readPermissionsLibraryId, libraryId)
		sendMessages.sendBroadcast(readPermissionsNeededIntent)
	}
}
