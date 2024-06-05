package com.lasthopesoftware.bluewater.shared.images.bytes

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.Promise
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class RemoteImageAccess(private val connectionProvider: ProvideLibraryConnections) : GetRawImages {
	companion object {
		private const val imageFormat = "jpg"

		private val logger by lazyLogger<RemoteImageAccess>()

		private val emptyByteArray by lazy { ByteArray(0) }
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> {
		val fileKey = serviceFile.key
		return Promise.Proxy { cp ->
			connectionProvider.promiseLibraryConnection(libraryId)
				.eventually { c ->
					c?.promiseResponse(
						"File/GetImage",
						"File=$fileKey",
						"Type=Full",
						"Pad=1",
						"Format=$imageFormat",
						"FillTransparency=ffffff"
					)?.also(cp::doCancel) ?: Promise.empty()
				}
				.then(
					{ response ->
						if (cp.isCancelled) throw CancellationException("Cancelled while retrieving image")
						else when (response?.code) {
							200 -> response.body.use { it.bytes() }
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

	override fun promiseImageBytes(libraryId: LibraryId, itemId: ItemId): Promise<ByteArray> {
		return Promise.Proxy { cp ->
			connectionProvider.promiseLibraryConnection(libraryId)
				.eventually { c ->
					c?.promiseResponse(
						"Browse/Image",
						"ID=${itemId.id}",
						"Type=Full",
						"Pad=1",
						"Format=$imageFormat",
						"FillTransparency=ffffff",
						"UseStackedImages=0",
						"Version=2",
					)?.also(cp::doCancel) ?: Promise.empty()
				}
				.then(
					{ response ->
						if (cp.isCancelled) throw java.util.concurrent.CancellationException("Cancelled while retrieving image")
						else when (response?.code) {
							200 -> response.body.use { it.bytes() }
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
								logger.error("There was an unexpected error getting the image for $itemId", e)

								throw e
							}
						}
					})
		}
	}
}
