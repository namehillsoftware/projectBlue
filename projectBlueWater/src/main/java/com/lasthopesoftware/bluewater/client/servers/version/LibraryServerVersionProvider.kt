package com.lasthopesoftware.bluewater.client.servers.version

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.io.promiseStandardResponse
import com.namehillsoftware.handoff.promises.Promise

class LibraryServerVersionProvider(private val libraryConnections: ProvideLibraryConnections) : ProvideLibraryServerVersion {
	override fun promiseServerVersion(libraryId: LibraryId): Promise<SemanticVersion?> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				connectionProvider
					?.promiseResponse("Alive")
					?.promiseStandardResponse()
					?.then { standardRequest ->
						standardRequest.items["ProgramVersion"]
							?.let { semVerString ->
								val semVerParts = semVerString.split(".")
								var major = 0
								var minor = 0
								var patch = 0
								if (semVerParts.isNotEmpty()) major = semVerParts[0].toInt()
								if (semVerParts.size > 1) minor = semVerParts[1].toInt()
								if (semVerParts.size > 2) patch = semVerParts[2].toInt()
								SemanticVersion(major, minor, patch)
							}
					}
					.keepPromise()
			}
}
