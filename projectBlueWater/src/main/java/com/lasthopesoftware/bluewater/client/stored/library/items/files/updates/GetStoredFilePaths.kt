package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.namehillsoftware.handoff.promises.Promise

interface GetStoredFilePaths {
	fun promiseStoredFilePath(): Promise<String?>
}
