package com.lasthopesoftware.resources.io

import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

class PromisingOutputStreamWrapper(private val outputStream: OutputStream) : PromisingOutputStream<PromisingOutputStreamWrapper> {
	override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int) = QueuedPromise(
		{
			outputStream.write(buffer, offset, length)
			this
		},
		ThreadPools.io
	)

	override fun flush(): Promise<PromisingOutputStreamWrapper> = QueuedPromise(
		{
			outputStream.flush()
			this
		},
		ThreadPools.io
	)

	override fun close() {
		outputStream.close()
	}

	fun promiseCopyFrom(inputStream: InputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE) = QueuedPromise(
		{ ct ->
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
		},
		ThreadPools.io
	)
}
