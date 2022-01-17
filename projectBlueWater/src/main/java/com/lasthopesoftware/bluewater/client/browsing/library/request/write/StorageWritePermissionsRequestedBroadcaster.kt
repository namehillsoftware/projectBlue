package com.lasthopesoftware.bluewater.client.browsing.library.request.write

import android.content.Intent
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder.Companion.buildMagicPropertyName
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

/**
 * Created by david on 7/3/16.
 */
class StorageWritePermissionsRequestedBroadcaster(private val sendMessages: SendMessages) :	IStorageWritePermissionsRequestedBroadcaster {
	override fun sendWritePermissionsNeededBroadcast(libraryId: Int) {
		val writePermissionsNeededIntent = Intent(writePermissionsNeeded)
		writePermissionsNeededIntent.putExtra(writePermissionsLibraryId, libraryId)
		sendMessages.sendBroadcast(writePermissionsNeededIntent)
	}

	companion object {
		val writePermissionsNeeded by lazy { buildMagicPropertyName<StorageWritePermissionsRequestedBroadcaster>("writePermissionsNeeded") }
		val writePermissionsLibraryId by lazy { buildMagicPropertyName<StorageWritePermissionsRequestedBroadcaster>("writePermissionsLibraryId") }
	}
}
