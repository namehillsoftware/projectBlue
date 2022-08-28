package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

object FileStringListUtilities {
	@JvmStatic
	fun promiseParsedFileStringList(fileList: String): Promise<Collection<ServiceFile>> {
		return QueuedPromise(
			MessageWriter { parseFileStringList(fileList) },
			ThreadPools.compute
		)
	}

	private fun parseFileStringList(fileList: String): Collection<ServiceFile> {
		val headerInfo = fileList.split(";", limit = 2)
		if (headerInfo.size < 2) return emptySet()
		val offset = headerInfo[0].toInt() + 1

		return fileList.splitToSequence(";")
			.drop(offset)
			.filter { it.isNotEmpty() && it != "-1" }
			.map { k -> ServiceFile(k.toInt()) }
			.toList()
	}

	@JvmStatic
	fun promiseSerializedFileStringList(serviceFiles: Collection<ServiceFile>): Promise<String> {
		return QueuedPromise(
			MessageWriter { serializeFileStringList(serviceFiles) },
			ThreadPools.compute
		)
	}

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
