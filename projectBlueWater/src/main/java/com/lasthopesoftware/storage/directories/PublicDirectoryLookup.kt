package com.lasthopesoftware.storage.directories

import android.content.Context
import android.os.Build
import android.os.Environment
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class PublicDirectoryLookup(private val context: Context) : GetPublicDirectories {
	override fun promisePublicDrives(): Promise<List<File>> {
		return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) Promise(listOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)))
		else Promise(listOf(*context.externalMediaDirs))
	}

}
