package com.lasthopesoftware.bluewater.client.browsing.items.access

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.PacketSender
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsValidation
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.shared.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder
import com.namehillsoftware.handoff.promises.Promise

class CachedItemProvider(
	private val inner: ProvideItems,
	private val revisions: CheckRevisions,
	private val functionCache: CachePromiseFunctions<Triple<LibraryId, Int, Int>, List<Item>>
) : ProvideItems {

	companion object {

		private val functionCache = LruPromiseCache<Triple<LibraryId, Int, Int>, List<Item>>(100)

		fun getInstance(context: Context): CachedItemProvider {
			val serverLookup = ServerLookup(ServerInfoXmlRequest(LibraryRepository(context), OkHttpFactory))
			val connectionSettingsLookup = ConnectionSettingsLookup(LibraryRepository(context))
			val libraryConnectionProvider = LibraryConnectionProvider(
				ConnectionSettingsValidation,
				connectionSettingsLookup,
				ServerAlarm(
					serverLookup,
					ServerWakeSignal(PacketSender()),
					AlarmConfiguration.standard
				),
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
			)

			return CachedItemProvider(
				ItemProvider(libraryConnectionProvider),
				LibraryRevisionProvider(libraryConnectionProvider),
				functionCache
			)
		}
	}

	override fun promiseItems(libraryId: LibraryId, itemKey: Int): Promise<List<Item>> =
		revisions.promiseRevision(libraryId)
			.eventually { revision ->
				functionCache.getOrAdd(Triple(libraryId, itemKey, revision)) { (l, k, _) -> inner.promiseItems(l, k) }
			}
}
