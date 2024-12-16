package com.lasthopesoftware.bluewater.shared.images

import com.namehillsoftware.handoff.promises.Promise

interface ProvideDefaultImage {
	fun promiseImageBytes(): Promise<ByteArray>
}
