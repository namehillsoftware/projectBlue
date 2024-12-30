package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

object FileStringListUtilities {
	fun promiseParsedFileStringList(fileList: String): Promise<Collection<ServiceFile>> = QueuedPromise(
		{ parseFileStringList(fileList) },
		ThreadPools.compute
	)

	private fun parseFileStringList(fileList: String): Collection<ServiceFile> {
		val headerInfo = fileList.split(";", limit = 3)
		if (headerInfo.size < 2) return emptySet()
		val offset = headerInfo[0].toInt() + 1
		val listSize = headerInfo[1].toInt()

		return headerInfo[2].splitToSequence(";")
			.drop(offset - 2)
			.filter { it.isNotEmpty() && it != "-1" }
			.map { k -> ServiceFile(k.toInt()) }
			.toCollection(ArrayList(listSize))
	}

	fun promiseSerializedFileStringList(serviceFiles: Collection<ServiceFile>): Promise<String> = QueuedPromise(
		{ serializeFileStringList(serviceFiles) },
		ThreadPools.compute
	)

	private fun serializeFileStringList(serviceFiles: Collection<ServiceFile>): String {
		val fileSize = serviceFiles.size
		// Take a guess that most keys will not be greater than 8 characters and add some more
		// for the first characters
		val sb = StringBuilder(fileSize * 9 + 8)
		sb.append("2;").append(fileSize).append(";-1;")
		for (serviceFile in serviceFiles) sb.append(serviceFile.key).append(";")
		return sb.toString()
	}
}
