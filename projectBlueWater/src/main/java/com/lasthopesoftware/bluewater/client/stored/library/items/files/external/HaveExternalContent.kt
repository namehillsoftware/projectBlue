package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import com.namehillsoftware.handoff.promises.Promise
import java.net.URI

interface HaveExternalContent {
	fun promiseNewContentUri(externalContent: ExternalContent): Promise<URI?>

	fun markContentAsNotPending(uri: URI): Promise<Unit>

	fun removeContent(uri: URI): Promise<Boolean>
}
