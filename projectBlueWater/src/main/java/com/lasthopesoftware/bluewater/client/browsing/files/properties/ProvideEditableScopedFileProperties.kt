package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface ProvideEditableScopedFileProperties {
	fun promiseFileProperties(serviceFile: ServiceFile): Promise<Sequence<FileProperty>>
}
