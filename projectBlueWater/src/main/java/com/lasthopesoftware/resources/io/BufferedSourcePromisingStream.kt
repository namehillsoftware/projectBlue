package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import okio.BufferedSource
import java.util.concurrent.Executor

/**
 * An implementation of a [PromisingReadableStream] that is backed by an [okio.BufferedSource].
 *
 * This class adapts a standard Okio `BufferedSource` into a promise-based asynchronous stream.
 * All read and close operations are performed asynchronously on a specified executor.
 *
 * @param bufferedSource The underlying [okio.BufferedSource] to read data from.
 * @param transferExecutor The [Executor] on which the asynchronous I/O operations (read, close) will be performed.
 *                         Defaults to [ThreadPools.io]. If null, will execute on calling thread.
 */
class BufferedSourcePromisingStream(
	private val bufferedSource: BufferedSource,
	private val transferExecutor: Executor? = ThreadPools.io,
) : PromisingReadableStream {
	override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = transferExecutor?.preparePromise {
		bufferedSource.read(b, off, len)
	} ?: bufferedSource.read(b, off, len).toPromise()

	override fun available(): Int = bufferedSource.buffer.size.toInt()

	override fun promiseClose(): Promise<Unit> = transferExecutor?.preparePromise {
		bufferedSource.close()
	} ?: bufferedSource.close().toPromise()

}
