package com.lasthopesoftware.bluewater.client.library.items.media.files.properties

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 3/14/17.
 */
interface ProvideFilePropertiesForSession {
	fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>>
}
