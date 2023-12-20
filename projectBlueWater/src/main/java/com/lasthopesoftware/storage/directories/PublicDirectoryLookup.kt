package com.lasthopesoftware.storage.directories

import android.content.Context
import android.os.Build
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class PublicDirectoryLookup(private val context: Context) : GetPublicDirectories {
	override fun promisePublicDrives(): Promise<Collection<File>> {
		return Promise(if (Build.VERSION.SDK_INT < 29) listOfNotNull(*context.externalMediaDirs) else emptyList())
	}
}
