package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.resources.io.PromisingReadableStream
import com.lasthopesoftware.resources.io.PromisingReadableStreamWrapper

class DeferredDownloadPromise : DeferredPromise<PromisingReadableStream> {
	constructor(bytes: ByteArray) : super(PromisingReadableStreamWrapper(bytes.inputStream(), null))
	constructor(throwable: Throwable) : super(throwable)
}
