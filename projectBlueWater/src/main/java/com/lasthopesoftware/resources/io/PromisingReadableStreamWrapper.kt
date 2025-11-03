package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.io.InputStream
import java.util.concurrent.Executor

class PromisingReadableStreamWrapper(
	private val inputStream: InputStream,
	private val transferExecutor: Executor = ThreadPools.io,
) : PromisingReadableStream {

    override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = transferExecutor.preparePromise {
		inputStream.read(b, off, len)
	}

	override fun available(): Int = inputStream.available()

	override fun promiseClose(): Promise<Unit> = transferExecutor.preparePromise { inputStream.close() }
}
