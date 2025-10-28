package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.io.InputStream

class PromisingReadableStreamWrapper(private val inputStream: InputStream) : PromisingReadableStream {
	override fun promiseRead(): Promise<Int> = inputStream.read().toPromise()

	override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = inputStream.read(b, off, len).toPromise()

	override fun available(): Int = inputStream.available()

	override fun promiseClose(): Promise<Unit> = inputStream.close().toPromise()
}
