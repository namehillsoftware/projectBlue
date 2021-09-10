package com.lasthopesoftware.storage.directories

import android.content.Context
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class PublicDirectoryLookup(private val context: Context) : GetPublicDirectories {
	override fun promisePublicDrives(): Promise<Collection<File>> {
		return Promise(listOf(*context.externalMediaDirs))
	}
}
