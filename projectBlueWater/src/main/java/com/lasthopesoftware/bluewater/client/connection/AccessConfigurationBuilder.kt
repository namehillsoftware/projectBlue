package com.lasthopesoftware.bluewater.client.connection

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object AccessConfigurationBuilder {
	private const val buildConnectionTimeoutTime = 10000

	@JvmStatic
	fun buildConfiguration(context: Context, library: Library): Promise<IUrlProvider> {
		val client = OkHttpClient.Builder().connectTimeout(buildConnectionTimeoutTime.toLong(), TimeUnit.MILLISECONDS).build()
		val serverLookup = ServerLookup(ServerInfoXmlRequest(LibraryRepository(context), client))
		val connectionTester = ConnectionTester()
		val urlScanner = UrlScanner(Base64Encoder(), connectionTester, serverLookup, OkHttpFactory.getInstance())

		return LiveUrlProvider(
			ActiveNetworkFinder(context),
			urlScanner).promiseLiveUrl(library)
	}
}
