package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats

import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider

class UpdatePlayStatsOnCompleteRegistration : IConnectionDependentReceiverRegistration {
    override fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): BroadcastReceiver {
        val cache = FilePropertyCache.getInstance()
		val scopedRevisionProvider = ScopedRevisionProvider(connectionProvider)
        return UpdatePlayStatsOnPlaybackCompleteReceiver(
            PlaystatsUpdateSelector(
                connectionProvider,
                ScopedFilePropertiesProvider(connectionProvider, scopedRevisionProvider, cache),
                ScopedFilePropertiesStorage(connectionProvider, scopedRevisionProvider, cache),
                ProgramVersionProvider(connectionProvider)
            )
        )
    }

    override fun forIntents(): Collection<IntentFilter> {
        return intents
    }

    companion object {
        private val intents: Collection<IntentFilter> =
            setOf(IntentFilter(PlaylistEvents.onPlaylistTrackComplete))
    }
}
