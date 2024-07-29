package com.lasthopesoftware.bluewater

import android.content.Context
import com.lasthopesoftware.bluewater.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.connection.PacketSender
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsValidation
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder

object ApplicationDependenciesContainer {

	private val serverWakeSignal by lazy { ServerWakeSignal(PacketSender()) }

	private val sync = Any()

	@Volatile
	private var attachedDependencies: AttachedDependencies? = null

	val Context.applicationDependencies: ApplicationDependencies
		// Double-checked initialization
		get() = attachedDependencies
			?.takeIf { it.context == applicationContext }
			?: synchronized(sync) {
				attachedDependencies
					?.takeIf { it.context == applicationContext }
					?: run {
						val newDependencies = AttachedDependencies(applicationContext)
						attachedDependencies = newDependencies
						newDependencies
					}
			}

	private class AttachedDependencies(val context: Context) : ApplicationDependencies {
		private val connectionRepository by lazy { PromisedConnectionsRepository() }

		override val intentBuilder by lazy { IntentBuilder(context) }

		override val syncScheduler by lazy { SyncScheduler(context) }

		override val sessionConnections by lazy {
			val serverLookup = ServerLookup(ServerInfoXmlRequest(LibraryRepository(context), OkHttpFactory))
			val connectionSettingsLookup = ConnectionSettingsLookup(LibraryRepository(context))

			ConnectionSessionManager(
				ConnectionTester,
				LibraryConnectionProvider(
					ConnectionSettingsValidation,
					connectionSettingsLookup,
					ServerAlarm(serverLookup, serverWakeSignal, AlarmConfiguration.standard),
					LiveUrlProvider(
						ActiveNetworkFinder(context),
						UrlScanner(
							Base64Encoder,
							ConnectionTester,
							serverLookup,
							connectionSettingsLookup,
							OkHttpFactory
						)
					),
					OkHttpFactory
				),
				connectionRepository,
				ApplicationMessageBus.getApplicationMessageBus(),
			)
		}
	}
}
