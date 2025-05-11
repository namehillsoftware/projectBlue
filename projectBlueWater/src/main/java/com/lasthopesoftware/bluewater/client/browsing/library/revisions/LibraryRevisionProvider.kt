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
	ImmediateResponse<Throwable, Long>
{
	companion object {
		private const val badRevision = -1L
	}

	override fun promiseRevision(libraryId: LibraryId): Promise<Long> = RevisionPromise(libraryId)

	override fun respond(resolution: Throwable?): Long = badRevision

	private inner class RevisionPromise(libraryId: LibraryId) :
		Promise.Proxy<Long>(),
		PromisedResponse<LiveServerConnection?, Long>
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

		override fun promiseResponse(connection: LiveServerConnection?): Promise<Long> =
			connection
				?.dataAccess
				?.promiseRevision()
				?.also(::doCancel)
				.keepPromise(badRevision)
				.then(NullIntFallbackResponse)
	}

	private object NullIntFallbackResponse : ImmediateResponse<Long?, Long> {
		override fun respond(resolution: Long?): Long = resolution ?: badRevision
	}
}
