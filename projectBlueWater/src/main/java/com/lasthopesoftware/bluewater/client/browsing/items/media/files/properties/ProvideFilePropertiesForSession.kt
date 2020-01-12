package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 3/14/17.
 */
interface ProvideFilePropertiesForSession {
	fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>>
}
