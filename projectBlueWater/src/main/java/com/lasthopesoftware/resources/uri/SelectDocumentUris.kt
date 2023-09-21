package com.lasthopesoftware.resources.uri

import com.namehillsoftware.handoff.promises.Promise
import java.net.URI

interface SelectDocumentUris {
	fun promiseSelectedDocumentUri(vararg mimeTypes: String): Promise<URI?>
}
