package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.resources.scheduling.ParsingScheduler
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

open class ImageProvider(private val selectedLibraryId: ProvideSelectedLibraryId, private val rawImages: GetRawImages) {
	open fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?> =
		selectedLibraryId.selectedLibraryId
			.eventually { libraryId ->
				libraryId
					?.let { l -> rawImages.promiseImageBytes(l, serviceFile) }
					?.eventually { bytes -> QueuedPromise(BitmapWriter(bytes), ParsingScheduler.instance().scheduler) }
					?: Promise.empty()
				}

	private class BitmapWriter(private val imageBytes: ByteArray) : MessageWriter<Bitmap?> {
		override fun prepareMessage(): Bitmap? =
			if (imageBytes.isNotEmpty()) BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
			else null
	}
}
