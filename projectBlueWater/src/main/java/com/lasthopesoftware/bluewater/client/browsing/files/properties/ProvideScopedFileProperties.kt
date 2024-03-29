package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 3/14/17.
 */
interface ProvideScopedFileProperties {
	fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>>
}
