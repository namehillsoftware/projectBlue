package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.io.PromisingWritableStream
import com.namehillsoftware.handoff.promises.Promise

object NullPromisingWritableStream : PromisingWritableStream {
	override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<Int> = length.toPromise()

	override fun promiseFlush(): Promise<Unit> = Unit.toPromise()
}
