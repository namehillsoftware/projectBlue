package com.lasthopesoftware.resources.io

import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.promises.Promise
import java.io.Closeable

interface PromisingReadableStream : Closeable, PromisingCloseable {
	fun promiseRead(): Promise<Int>
	fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int>
	fun available(): Int
}

