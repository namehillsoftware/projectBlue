package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats

import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.PlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.receivers.RegisterReceiverForEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage.TrackCompleted
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages

class UpdatePlayStatsOnCompleteRegistration(private val messageBus: SendApplicationMessages) : RegisterReceiverForEvents {
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
					cache,
					messageBus,
				),
				ProgramVersionProvider(connectionProvider)
			)
		)
	}

	override fun forClasses(): Collection<Class<out ApplicationMessage>> = classes

	companion object {
		private val classes = setOf<Class<out ApplicationMessage>>(cls<TrackCompleted>())
	}
}
