package com.lasthopesoftware.bluewater.client.browsing.library.request.write

import android.content.Intent
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder.Companion.buildMagicPropertyName
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

/**
 * Created by david on 7/3/16.
 */
class StorageWritePermissionsRequestedBroadcaster(private val sendMessages: SendMessages) :	IStorageWritePermissionsRequestedBroadcaster {
	override fun sendWritePermissionsNeededBroadcast(libraryId: Int) {
		val writePermissionsNeededIntent = Intent(WritePermissionsNeeded)
		writePermissionsNeededIntent.putExtra(WritePermissionsLibraryId, libraryId)
		sendMessages.sendBroadcast(writePermissionsNeededIntent)
	}

	companion object {
		val WritePermissionsNeeded = buildMagicPropertyName(
			StorageWritePermissionsRequestedBroadcaster::class.java, "WritePermissionsNeeded"
		)
		val WritePermissionsLibraryId = buildMagicPropertyName(
			StorageWritePermissionsRequestedBroadcaster::class.java, "WritePermissionsLibraryId"
		)
	}
}
