package com.lasthopesoftware.bluewater.client.library.items.media.files.access

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider
import com.namehillsoftware.handoff.promises.Promise

class FileProvider(private val fileStringListProvider: FileStringListProvider) : AbstractFileResponder(), ProvideFiles {
	override fun promiseFiles(option: FileListParameters.Options, vararg params: String): Promise<List<ServiceFile>> {
		return fileStringListProvider
			.promiseFileStringList(option, *params)
			.eventually(this)
			.then(this)
	}
}
