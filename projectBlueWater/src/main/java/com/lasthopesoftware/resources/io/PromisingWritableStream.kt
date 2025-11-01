package com.lasthopesoftware.resources.io

import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.promises.Promise

interface PromisingWritableStream : PromisingCloseable {
	fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<Int>
	fun promiseFlush(): Promise<Unit>

	override fun promiseClose() = promiseFlush()
}
