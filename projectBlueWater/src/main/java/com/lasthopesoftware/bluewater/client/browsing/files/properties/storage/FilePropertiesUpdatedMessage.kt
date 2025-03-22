package com.lasthopesoftware.bluewater.client.browsing.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

data class FilePropertiesUpdatedMessage(
	val urlServiceKey: UrlKeyHolder<ServiceFile>
) : ApplicationMessage
