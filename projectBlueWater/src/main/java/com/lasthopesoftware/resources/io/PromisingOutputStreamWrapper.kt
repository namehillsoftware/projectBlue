package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

class PromisingOutputStreamWrapper(private val outputStream: OutputStream) : PromisingOutputStream<PromisingOutputStreamWrapper> {
	override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int) = ThreadPools.io.preparePromise {
		outputStream.write(buffer, offset, length)
		this
	}

	override fun flush(): Promise<PromisingOutputStreamWrapper> = ThreadPools.io.preparePromise {
		outputStream.flush()
		this
	}

	override fun close() {
		outputStream.close()
	}

	fun promiseCopyFrom(inputStream: InputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE) = ThreadPools.io.preparePromise { ct ->
		val buffer = ByteArray(bufferSize)

		if (ct.isCancelled) throw CancellationException("promiseCopyFrom was cancelled.")
		var bytes = inputStream.read(buffer)
		while (bytes >= 0) {
			if (ct.isCancelled) throw CancellationException("promiseCopyFrom was cancelled.")
			outputStream.write(buffer, 0, bytes)
			if (ct.isCancelled) throw CancellationException("promiseCopyFrom was cancelled.")
			bytes = inputStream.read(buffer)
		}
		this
	}
}
