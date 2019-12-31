package com.lasthopesoftware.storage.directories

import com.namehillsoftware.handoff.promises.Promise
import java.io.File

interface GetPublicDirectories {
	fun promisePublicDrives(): Promise<List<File>>
}
