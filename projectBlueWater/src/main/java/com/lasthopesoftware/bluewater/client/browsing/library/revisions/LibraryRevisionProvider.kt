package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.access.ProvideRemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.net.URL

class LibraryRevisionProvider(
	private val libraryAccess: ProvideRemoteLibraryAccess,
	private val libraryConnections: ProvideLibraryConnections,
	private val serverRevisionData: HasServerRevisionData,
) : CheckRevisions, ImmediateResponse<Throwable, Int>, PromisedResponse<RemoteLibraryAccess?, Int> {
	companion object {
		private const val badRevision = -1
	}

	override fun promiseRevision(libraryId: LibraryId): Promise<Int> = RevisionPromise(libraryId)

	override fun respond(resolution: Throwable?): Int = badRevision

	override fun promiseResponse(access: RemoteLibraryAccess?): Promise<Int> =
		access?.promiseRevision().keepPromise(badRevision).then(NullIntFallbackResponse)

	private inner class RevisionPromise(private val libraryId: LibraryId): Promise.Proxy<Int>(), PromisedResponse<ProvideConnections?, Int>, (URL) -> Promise<Int> {
		init {
			proxy(
				libraryConnections
					.promiseLibraryConnection(libraryId)
					.also(::doCancel)
					.eventually(this)
					.then(forward(), this@LibraryRevisionProvider)
			)
		}

		override fun promiseResponse(connection: ProvideConnections?): Promise<Int> =
			connection
				?.serverConnection
				?.baseUrl
				?.let { url -> serverRevisionData.getOrSetRevisionData(url, this) }
				.keepPromise(badRevision)

		override fun invoke(url: URL): Promise<Int> =
			libraryAccess
				.promiseLibraryAccess(libraryId)
				.also(::doCancel)
				.eventually(this@LibraryRevisionProvider)
	}

	private object NullIntFallbackResponse : ImmediateResponse<Int?, Int> {
		override fun respond(resolution: Int?): Int = resolution ?: badRevision
	}
}
