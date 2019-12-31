package com.lasthopesoftware.storage.directories

import android.content.Context
import android.os.Environment
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class PrivateDirectoryLookup(private val context: Context) : GetPrivateDirectories {
	override fun promisePrivateDrives(): Promise<List<File>> {
		return Promise(listOf(*context.getExternalFilesDirs(Environment.DIRECTORY_MUSIC)))
	}

}
