package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse

class LibraryRevisionProvider(private val libraryAccess: ProvideLibraryConnections) :
	CheckRevisions,
	ImmediateResponse<Throwable, Int>
{
	companion object {
		private const val badRevision = -1
	}

	override fun promiseRevision(libraryId: LibraryId): Promise<Int> = RevisionPromise(libraryId)

	override fun respond(resolution: Throwable?): Int = badRevision

	private inner class RevisionPromise(libraryId: LibraryId) :
		Promise.Proxy<Int>(),
		PromisedResponse<LiveServerConnection?, Int>
	{
		init {
			proxy(
				libraryAccess
					.promiseLibraryConnection(libraryId)
					.also(::doCancel)
					.eventually(this)
					.then(forward(), this@LibraryRevisionProvider)
			)
		}

		override fun promiseResponse(connection: LiveServerConnection?): Promise<Int> =
			connection
				?.dataAccess
				?.promiseRevision()
				?.also(::doCancel)
				.keepPromise(badRevision)
				.then(NullIntFallbackResponse)
	}

	private object NullIntFallbackResponse : ImmediateResponse<Int?, Int> {
		override fun respond(resolution: Int?): Int = resolution ?: badRevision
	}
}
