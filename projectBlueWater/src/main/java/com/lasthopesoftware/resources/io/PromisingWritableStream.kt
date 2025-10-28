package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.promises.Promise

interface PromisingWritableStream<T : PromisingWritableStream<T>> : PromisingCloseable {
	fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<T>
	fun flush(): Promise<T>

	override fun promiseClose() = flush().unitResponse()
}
