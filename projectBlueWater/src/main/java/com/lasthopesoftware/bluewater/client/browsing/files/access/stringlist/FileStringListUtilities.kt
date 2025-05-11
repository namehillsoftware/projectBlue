package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import kotlin.coroutines.cancellation.CancellationException

object FileStringListUtilities {
	fun promiseParsedFileStringList(fileList: String): Promise<Collection<ServiceFile>> =
		ThreadPools.compute.preparePromise { parseFileStringList(fileList, it) }

	private fun parseFileStringList(fileList: String, cancellationSignal: CancellationSignal): Collection<ServiceFile> {
		if (cancellationSignal.isCancelled) throw parsingCancelledException()

		val headerInfo = fileList.split(";", limit = 3)
		if (headerInfo.size < 2) return emptySet()

		if (cancellationSignal.isCancelled) throw parsingCancelledException()

		val offset = headerInfo[0].toInt() + 1
		val listSize = headerInfo[1].toInt()

		if (cancellationSignal.isCancelled) throw parsingCancelledException()

		return headerInfo[2].splitToSequence(";")
			.drop(offset - 2)
			.filter {
				if (cancellationSignal.isCancelled) throw parsingCancelledException()

				it.isNotEmpty() && it != "-1"
			}
			.map { k ->
				if (cancellationSignal.isCancelled) throw parsingCancelledException()

				ServiceFile(k)
			}
			.toCollection(ArrayList(listSize))
	}

	fun promiseSerializedFileStringList(serviceFiles: Collection<ServiceFile>): Promise<String> =
		ThreadPools.compute.preparePromise { serializeFileStringList(serviceFiles, it) }

	fun promiseShuffledSerializedFileStringList(serviceFiles: Collection<ServiceFile>): Promise<String> =
		ThreadPools.compute.preparePromise { serializeFileStringList(serviceFiles.shuffled(), it) }

	private fun serializeFileStringList(serviceFiles: Collection<ServiceFile>, cancellationSignal: CancellationSignal): String {
		if (cancellationSignal.isCancelled) throw serializingCancelledException()

		val fileSize = serviceFiles.size
		// Take a guess that most keys will not be greater than 8 characters and add some more
		// for the first characters
		val sb = StringBuilder(fileSize * 9 + 8)
		sb.append("2;").append(fileSize).append(";-1;")

		for (serviceFile in serviceFiles) {
			if (cancellationSignal.isCancelled) throw serializingCancelledException()
			sb.append(serviceFile.key).append(";")
		}

		return sb.toString()
	}

	private fun parsingCancelledException() = CancellationException("Parsing cancelled.")
	private fun serializingCancelledException() = CancellationException("Serialization cancelled.")
}
