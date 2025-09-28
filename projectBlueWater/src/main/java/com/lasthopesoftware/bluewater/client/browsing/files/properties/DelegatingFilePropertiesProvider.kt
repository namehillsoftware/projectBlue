package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.policies.ExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

class DelegatingFilePropertiesProvider(private val inner: ProvideFreshLibraryFileProperties, private val policy: ExecutionPolicies) : ProvideFreshLibraryFileProperties {

	private val filePropertiesFunction by lazy { policy.applyPolicy(inner::promiseFileProperties) }

	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<LookupFileProperties> =
		filePropertiesFunction(libraryId, serviceFile)
}
