package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.pebble

import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents

class PebbleFileChangedNotificationRegistration : IConnectionDependentReceiverRegistration {

	companion object {
		private val intents: Collection<IntentFilter> =
			setOf(IntentFilter(PlaylistEvents.onPlaylistTrackChange))
	}

    override fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): ReceiveBroadcastEvents {
        val filePropertiesProvider = ScopedCachedFilePropertiesProvider(
            connectionProvider,
            FilePropertyCache.getInstance(),
            ScopedFilePropertiesProvider(
                connectionProvider,
				ScopedRevisionProvider(connectionProvider),
                FilePropertyCache.getInstance()
            )
        )
        return PebbleFileChangedProxy(filePropertiesProvider)
    }

    override fun forIntents(): Collection<IntentFilter> = intents
}
