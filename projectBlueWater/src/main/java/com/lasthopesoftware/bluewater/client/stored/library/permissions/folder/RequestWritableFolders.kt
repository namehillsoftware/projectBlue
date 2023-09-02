package com.lasthopesoftware.bluewater.client.stored.library.permissions.folder

import com.namehillsoftware.handoff.promises.Promise
import java.net.URI

interface RequestWritableFolders {
	fun promiseWritableFolder(): Promise<URI?>
}
