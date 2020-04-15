package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException

open class RemoteImageAccess(private val connectionProvider: IConnectionProvider) : GetRawImages {
	override fun promiseImageBytes(serviceFile: ServiceFile): Promise<ByteArray> {
		val fileKey = serviceFile.key
		return connectionProvider.promiseResponse("File/GetImage", "File=$fileKey", "Type=Full", "Pad=1", "Format=$IMAGE_FORMAT", "FillTransparency=ffffff")
			.then(
				{ response -> response.body?.use { it.bytes() } },
				{ e ->
					when (e) {
						is FileNotFoundException -> {
							logger.warn("Image not found!")
							ByteArray(0)
						}
						is IOException -> {
							logger.error("There was an error getting the connection for images", e)
							ByteArray(0)
						}
						else -> {
							logger.error("There was an unexpected error getting the image for $fileKey", e)

							throw e
						}
					}
				})
	}

	companion object {
		private const val IMAGE_FORMAT = "jpg"

		private val logger = LoggerFactory.getLogger(RemoteImageAccess::class.java)
	}
}
