package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.namehillsoftware.handoff.promises.Promise

class FilePropertiesProvider(
	private val libraryConnections: ProvideGuaranteedLibraryConnections
) : ProvideFreshLibraryFileProperties {

	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<LookupFileProperties> =
		libraryConnections
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { access ->
				access.promiseFileProperties(serviceFile)
			}
}
