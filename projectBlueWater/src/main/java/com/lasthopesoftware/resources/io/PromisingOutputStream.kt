package com.lasthopesoftware.resources.io

import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.promises.Promise

interface PromisingOutputStream<T : PromisingOutputStream<T>> : PromisingCloseable {
	fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<T>
	fun flush(): Promise<T>
}
