package com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface LookupImageCacheKey {
	fun promiseImageCacheKey(serviceFile: ServiceFile): Promise<String>
}
