package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import okio.BufferedSource
import java.util.concurrent.Executor

class BufferedSourcePromisingStream(
	private val bufferedSource: BufferedSource,
	private val transferExecutor: Executor = ThreadPools.io,
) : PromisingReadableStream {
	override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = transferExecutor.preparePromise {
		bufferedSource.read(b, off, len)
	}

	override fun available(): Int = bufferedSource.buffer.size.toInt()

	override fun promiseClose(): Promise<Unit> = transferExecutor.preparePromise {
		bufferedSource.close()
	}

}
