package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface GetRawImages {
	fun promiseImageBytes(serviceFile: ServiceFile): Promise<ByteArray>
}
