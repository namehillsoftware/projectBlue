package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException

class RemoteImageAccess(private val connectionProvider: ProvideLibraryConnections) : GetRawImages {
	companion object {
		private const val IMAGE_FORMAT = "jpg"

		private val logger = LoggerFactory.getLogger(RemoteImageAccess::class.java)

		private val emptyByteArray = lazy { ByteArray(0) }
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> {
		val fileKey = serviceFile.key
		return connectionProvider.promiseLibraryConnection(libraryId)
			.eventually { c -> c?.promiseResponse("File/GetImage", "File=$fileKey", "Type=Full", "Pad=1", "Format=$IMAGE_FORMAT", "FillTransparency=ffffff") ?: Promise.empty() }
			.then(
				{ response ->
					when (response.code) {
						200 -> response.body?.use { it.bytes() } ?: emptyByteArray.value
						else -> emptyByteArray.value
					}
				},
				{ e ->
					when (e) {
						is FileNotFoundException -> {
							logger.warn("Image not found!")
							emptyByteArray.value
						}
						is IOException -> {
							logger.error("There was an error getting the connection for images", e)
							emptyByteArray.value
						}
						else -> {
							logger.error("There was an unexpected error getting the image for $fileKey", e)

							throw e
						}
					}
				})
	}
}
