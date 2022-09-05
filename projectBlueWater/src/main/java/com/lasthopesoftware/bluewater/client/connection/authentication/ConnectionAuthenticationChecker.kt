package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.StandardRequest
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class ConnectionAuthenticationChecker(private val libraryConnections: ProvideLibraryConnections) : CheckIfConnectionIsReadOnly {
	override fun promiseIsReadOnly(libraryId: LibraryId): Promise<Boolean> = libraryConnections
		.promiseLibraryConnection(libraryId)
		.eventually { it?.promiseResponse("Authenticate").keepPromise() }
		.then { r ->
			r?.body
				?.use { b -> b.byteStream().use(StandardRequest::fromInputStream) }
				?.let { sr -> sr.items["ReadOnly"]?.toInt() }
				?.let { ro -> ro != 0 }
				?: false
		}
		?: false.toPromise()
}
