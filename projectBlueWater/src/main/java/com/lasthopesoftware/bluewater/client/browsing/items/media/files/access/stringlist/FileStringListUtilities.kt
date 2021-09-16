package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.util.*

object FileStringListUtilities {
    @JvmStatic
	fun promiseParsedFileStringList(fileList: String): Promise<Collection<ServiceFile>> {
        return QueuedPromise(
            MessageWriter { parseFileStringList(fileList) },
            ThreadPools.compute
        )
    }

    private fun parseFileStringList(fileList: String): Collection<ServiceFile> {
        val keys = fileList.split(";").toTypedArray()
        if (keys.size < 2) return emptySet()
        val offset = keys[0].toInt() + 1
        val serviceFiles = ArrayList<ServiceFile>(keys[1].toInt())
        for (i in offset until keys.size) {
            if (keys[i] == "-1") continue
            serviceFiles.add(ServiceFile(keys[i].toInt()))
        }
        return serviceFiles
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
