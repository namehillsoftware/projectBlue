package com.lasthopesoftware.bluewater.shared.images.bytes

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.io.FileNotFoundException
import java.io.IOException

class RemoteImageAccess(private val libraryConnections: ProvideLibraryConnections) : GetImageBytes {
	companion object {
		private val logger by lazyLogger<RemoteImageAccess>()
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> =
		object : Promise.Proxy<ByteArray>(), PromisedResponse<LiveServerConnection?, ByteArray>, ImmediateResponse<Throwable, ByteArray> {
			init {
				proxy(
					libraryConnections
						.promiseLibraryConnection(libraryId)
						.eventually(this)
						.also(::doCancel)
						.then(forward(), this)
				)
			}

			override fun promiseResponse(connection: LiveServerConnection?): Promise<ByteArray> =
				connection?.dataAccess?.promiseImageBytes(serviceFile).keepPromise(emptyByteArray)

			override fun respond(error: Throwable): ByteArray =
				when (error) {
					is FileNotFoundException -> {
						logger.warn("Image not found!")
						emptyByteArray
					}
					is IOException -> {
						logger.error("There was an error getting the connection for images", error)
						emptyByteArray
					}
					else -> {
						logger.error("There was an unexpected error getting the image for $serviceFile", error)

						throw error
					}
				}
		}

	override fun promiseImageBytes(libraryId: LibraryId, itemId: ItemId): Promise<ByteArray> =
		object : Promise.Proxy<ByteArray>(), PromisedResponse<LiveServerConnection?, ByteArray>, ImmediateResponse<Throwable, ByteArray> {
			init {
				proxy(
					libraryConnections
						.promiseLibraryConnection(libraryId)
						.eventually(this)
						.also(::doCancel)
						.then(forward(), this)
				)
			}

			override fun promiseResponse(connection: LiveServerConnection?): Promise<ByteArray> =
				connection?.dataAccess?.promiseImageBytes(itemId).keepPromise(emptyByteArray)

			override fun respond(error: Throwable): ByteArray =
				when (error) {
					is FileNotFoundException -> {
						logger.warn("Image not found!")
						emptyByteArray
					}
					is IOException -> {
						logger.error("There was an error getting the connection for images", error)
						emptyByteArray
					}
					else -> {
						logger.error("There was an unexpected error getting the image for $itemId", error)

						throw error
					}
				}
		}
}
