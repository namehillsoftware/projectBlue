package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckScopedRevisions
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

private val promisedEmptyProperties by lazy { emptyMap<String, String>().toPromise() }

class ScopedFilePropertiesProvider(private val scopedConnection: IConnectionProvider, private val checkScopedRevisions: CheckScopedRevisions, private val filePropertiesContainerProvider: IFilePropertiesContainerRepository) : ProvideScopedFileProperties {
	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		CancellableProxyPromise { cp ->
			checkScopedRevisions.promiseRevision().also(cp::doCancel).eventually { revision ->
				if (cp.isCancelled) promisedEmptyProperties
				else FilePropertiesPromise(scopedConnection, filePropertiesContainerProvider, serviceFile, revision)
			}
		}
}
