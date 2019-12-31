package com.lasthopesoftware.storage.directories

import com.namehillsoftware.handoff.promises.Promise
import java.io.File

interface GetPrivateDirectories {
	fun promisePrivateDrives(): Promise<List<File>>
}
