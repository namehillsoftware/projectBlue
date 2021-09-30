package com.lasthopesoftware.bluewater.client.stored.library.items.files.download

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.IServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.ResponseBody
import java.io.ByteArrayInputStream
import java.io.InputStream

class StoredFileDownloader(private val serviceFileUriQueryParamsProvider: IServiceFileUriQueryParamsProvider, private val libraryConnections: ProvideLibraryConnections) : DownloadStoredFiles {
	override fun promiseDownload(libraryId: LibraryId, storedFile: StoredFile): Promise<InputStream> =
		CancellableProxyPromise { cp ->
			val promisedResponse = libraryConnections
				.promiseLibraryConnection(libraryId)
				.eventually { c -> c?.promiseResponse(*serviceFileUriQueryParamsProvider.getServiceFileUriQueryParams(ServiceFile(storedFile.serviceId))) ?: Promise.empty() }

			cp.doCancel(promisedResponse)

			promisedResponse
				.then { r ->
					r?.body
						?.takeIf { r.code != 404 }
						?.let(::StreamedResponse)
						?: ByteArrayInputStream(ByteArray(0))
				}
		}

	private class StreamedResponse(private val responseBody: ResponseBody) : InputStream() {
		private val byteStream: InputStream = responseBody.byteStream()

		override fun read(): Int {
			return byteStream.read()
		}

		override fun read(b: ByteArray, off: Int, len: Int): Int {
			return byteStream.read(b, off, len)
		}

		override fun available(): Int {
			return byteStream.available()
		}

		override fun close() {
			byteStream.close()
			responseBody.close()
		}

		override fun toString(): String {
			return byteStream.toString()
		}
	}
}
