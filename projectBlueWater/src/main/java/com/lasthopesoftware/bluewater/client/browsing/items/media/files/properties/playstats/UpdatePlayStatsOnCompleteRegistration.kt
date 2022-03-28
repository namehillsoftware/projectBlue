package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats

import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.receivers.RegisterReceiverForEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistMessages.TrackCompleted
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

class UpdatePlayStatsOnCompleteRegistration : RegisterReceiverForEvents {
	override fun registerBroadcastEventsWithConnectionProvider(connectionProvider: IConnectionProvider): ReceiveBroadcastEvents {
		return ReceiveBroadcastEvents {  }
	}

	override fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): (ApplicationMessage) -> Unit {
		val cache = FilePropertyCache.getInstance()
		val scopedRevisionProvider = ScopedRevisionProvider(connectionProvider)
		return UpdatePlayStatsOnPlaybackCompleteReceiver(
			PlaystatsUpdateSelector(
				connectionProvider,
				ScopedFilePropertiesProvider(connectionProvider, scopedRevisionProvider, cache),
				ScopedFilePropertiesStorage(
					connectionProvider,
					ScopedConnectionAuthenticationChecker(connectionProvider),
					scopedRevisionProvider,
					cache
				),
				ProgramVersionProvider(connectionProvider)
			)
		)
	}

	override fun forIntents(): Collection<IntentFilter> = emptySet()

	@Suppress("UNCHECKED_CAST")
	override fun forClasses(): Collection<Class<ApplicationMessage>> = classes as Collection<Class<ApplicationMessage>>

	companion object {
		private val classes = setOf<Class<*>>(cls<TrackCompleted>())
	}
}
