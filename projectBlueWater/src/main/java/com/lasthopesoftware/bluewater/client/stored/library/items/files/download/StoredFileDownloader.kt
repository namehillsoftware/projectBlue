package com.lasthopesoftware.bluewater.client.stored.library.items.files.download

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.IServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.RejectionProxy
import com.namehillsoftware.handoff.promises.propagation.ResolutionProxy
import okhttp3.ResponseBody
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class StoredFileDownloader(private val serviceFileUriQueryParamsProvider: IServiceFileUriQueryParamsProvider, private val libraryConnections: ProvideLibraryConnections) : DownloadStoredFiles {
	override fun promiseDownload(libraryId: LibraryId, storedFile: StoredFile): Promise<InputStream> {
		return Promise { m ->
			val cancellationProxy = CancellationProxy()
			m.cancellationRequested(cancellationProxy)

			val promisedResponse = libraryConnections
				.promiseLibraryConnection(libraryId)
				.eventually { c -> c.promiseResponse(*serviceFileUriQueryParamsProvider.getServiceFileUriQueryParams(ServiceFile(storedFile.serviceId))) }

			cancellationProxy.doCancel(promisedResponse)

			promisedResponse
				.then { r ->
					val body = r.body
					if (body == null || r.code == 404) ByteArrayInputStream(ByteArray(0))
					else StreamedResponse(body)
				}
				.then(ResolutionProxy(m), RejectionProxy(m))
		}
	}

	private class StreamedResponse internal constructor(private val responseBody: ResponseBody) : InputStream() {
		private val byteStream: InputStream = responseBody.byteStream()
		@Throws(IOException::class)
		override fun read(): Int {
			return byteStream.read()
		}

		@Throws(IOException::class)
		override fun read(b: ByteArray, off: Int, len: Int): Int {
			return byteStream.read(b, off, len)
		}

		@Throws(IOException::class)
		override fun available(): Int {
			return byteStream.available()
		}

		@Throws(IOException::class)
		override fun close() {
			byteStream.close()
			responseBody.close()
		}

		override fun toString(): String {
			return byteStream.toString()
		}
	}
}
