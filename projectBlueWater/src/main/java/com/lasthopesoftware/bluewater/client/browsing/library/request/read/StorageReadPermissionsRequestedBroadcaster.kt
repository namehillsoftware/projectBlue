package com.lasthopesoftware.bluewater.client.browsing.library.request.read

import android.content.Intent
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder.Companion.buildMagicPropertyName
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

/**
 * Created by david on 7/3/16.
 */
class StorageReadPermissionsRequestedBroadcaster(private val sendMessages: SendMessages) : IStorageReadPermissionsRequestedBroadcast {

	companion object {
		val ReadPermissionsNeeded = buildMagicPropertyName(
			StorageReadPermissionsRequestedBroadcaster::class.java, "ReadPermissionsNeeded"
		)
		val ReadPermissionsLibraryId = buildMagicPropertyName(
			StorageReadPermissionsRequestedBroadcaster::class.java, "ReadPermissionsLibraryId"
		)
	}

	override fun sendReadPermissionsRequestedBroadcast(libraryId: Int) {
		val readPermissionsNeededIntent = Intent(ReadPermissionsNeeded)
		readPermissionsNeededIntent.putExtra(ReadPermissionsLibraryId, libraryId)
		sendMessages.sendBroadcast(readPermissionsNeededIntent)
	}
}
