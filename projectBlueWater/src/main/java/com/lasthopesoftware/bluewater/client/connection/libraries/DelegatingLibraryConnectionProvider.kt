package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.policies.ApplyExecutionPolicies
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import okhttp3.Response

class DelegatingLibraryConnectionProvider(
	private val inner: ProvideLibraryConnections,
	private val policies: ApplyExecutionPolicies
) : ProvideLibraryConnections {
	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, ProvideConnections?>(),
			ImmediateResponse<ProvideConnections?, Unit> {
			init {
				val promisedConnection = inner.promiseLibraryConnection(libraryId)
				doCancel(promisedConnection)
				proxyProgress(promisedConnection)
				proxyRejection(promisedConnection)
				promisedConnection.then(this)
			}

			override fun respond(connections: ProvideConnections?) {
				resolve(connections?.let { DelegatingConnectionProvider(it, policies) })
			}
		}

	private class DelegatingConnectionProvider(
		private val inner: ProvideConnections,
		policies: ApplyExecutionPolicies
	) : ProvideConnections by inner {
		private val responseProvider by lazy { policies.applyPolicy(inner::promiseResponse) }

		override fun promiseResponse(vararg params: String): Promise<Response> = responseProvider(params)
	}
}
