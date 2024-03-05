package com.lasthopesoftware.bluewater.client.browsing.files.image.bytes

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.cls
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.ProxyPromise
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class RemoteImageAccess(private val connectionProvider: ProvideLibraryConnections) : GetRawImages {
	companion object {
		private const val IMAGE_FORMAT = "jpg"

		private val logger by lazy { LoggerFactory.getLogger(cls<RemoteImageAccess>()) }

		private val emptyByteArray by lazy { ByteArray(0) }
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> {
		val fileKey = serviceFile.key
		return ProxyPromise { cp ->
			connectionProvider.promiseLibraryConnection(libraryId)
				.eventually { c ->
					c?.promiseResponse(
						"File/GetImage",
						"File=$fileKey",
						"Type=Full",
						"Pad=1",
						"Format=$IMAGE_FORMAT",
						"FillTransparency=ffffff"
					)?.also(cp::doCancel) ?: Promise.empty()
				}
				.then(
					{ response ->
						if (cp.isCancelled) throw CancellationException("Cancelled while retrieving image")
						else when (response?.code) {
							200 -> response.body?.use { it.bytes() } ?: emptyByteArray
							else -> emptyByteArray
						}
					},
					{ e ->
						when (e) {
							is FileNotFoundException -> {
								logger.warn("Image not found!")
								emptyByteArray
							}
							is IOException -> {
								logger.error("There was an error getting the connection for images", e)
								emptyByteArray
							}
							else -> {
								logger.error("There was an unexpected error getting the image for $fileKey", e)

								throw e
							}
						}
					})
		}
	}
}
