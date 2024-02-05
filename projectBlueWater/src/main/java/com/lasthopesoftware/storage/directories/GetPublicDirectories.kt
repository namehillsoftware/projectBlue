package com.lasthopesoftware.storage.directories

import com.namehillsoftware.handoff.promises.Promise
import java.io.File

fun interface GetPublicDirectories {
	fun promisePublicDrives(): Promise<Collection<File>>
}
