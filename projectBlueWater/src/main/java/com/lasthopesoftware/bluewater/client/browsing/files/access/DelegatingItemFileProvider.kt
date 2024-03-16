package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.policies.ApplyExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

class DelegatingItemFileProvider(private val inner: ProvideItemFiles, policies: ApplyExecutionPolicies) : ProvideItemFiles {
	private val promisedFiles = policies.applyPolicy(inner::promiseFiles)

	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId?, options: FileListParameters.Options): Promise<List<ServiceFile>> =
		promisedFiles(libraryId, itemId, options)
}
