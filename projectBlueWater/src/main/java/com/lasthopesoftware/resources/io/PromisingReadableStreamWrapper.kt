package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.PromisingReadableStream.Companion.readingCancelledException
import com.namehillsoftware.handoff.promises.Promise
import java.io.InputStream
import java.util.concurrent.Executor

/**
 * An implementation of a [PromisingReadableStream] that is backed by an [InputStream].
 *
 * This class adapts a standard InputStream into a promise-based asynchronous stream.
 * All read and close operations are performed asynchronously on a specified executor.
 *
 * @param inputStream The underlying [InputStream] to read data from.
 * @param transferExecutor The [Executor] on which the asynchronous I/O operations (read, close) will be performed.
 *                         Defaults to [ThreadPools.io]. If null, will execute on calling thread.
 */
class PromisingReadableStreamWrapper(
	private val inputStream: InputStream,
	private val transferExecutor: Executor? = ThreadPools.io,
) : PromisingReadableStream {

    override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = transferExecutor?.preparePromise { cs ->
		if (!cs.isCancelled) inputStream.read(b, off, len)
		else throw readingCancelledException()
	} ?: inputStream.read(b, off, len).toPromise()

	override fun available(): Int = inputStream.available()

	override fun promiseClose(): Promise<Unit> = transferExecutor?.preparePromise { inputStream.close() }
		?: inputStream.close().toPromise()
}
