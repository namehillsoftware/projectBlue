package com.lasthopesoftware.storage.directories

import com.lasthopesoftware.storage.GetFreeSpace
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class FakePublicDirectoryLookup : GetPublicDirectories, GetFreeSpace {
	private val files = ArrayList<FreeSpaceFile>()

	override fun promisePublicDrives(): Promise<Collection<File>> = Promise(files)

	fun addDirectory(filePath: String, freeSpace: Long) {
		files.add(FreeSpaceFile(filePath, freeSpace))
	}

	override fun getFreeSpace(file: File): Long {
		var path = file.path
		val filePaths = files.map { it.path }
		while (!filePaths.contains(path)) {
			val pathSeparatorIndex = path.lastIndexOf('/')
			if (pathSeparatorIndex < 0) return 0
			path = path.substring(0, pathSeparatorIndex)
		}
		val matchingPath = path
		val freeSpaceFile = files.firstOrNull { it.path == matchingPath }
		return freeSpaceFile?.freeSpace ?: 0
	}
}
