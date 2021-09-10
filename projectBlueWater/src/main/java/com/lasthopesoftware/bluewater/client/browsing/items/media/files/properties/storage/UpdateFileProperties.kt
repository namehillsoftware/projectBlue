package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface UpdateFileProperties {
	fun promiseFileUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit>
}
