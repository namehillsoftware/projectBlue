package com.lasthopesoftware.bluewater.client.playback.exoplayer

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException
import androidx.media3.datasource.HttpDataSource.RequestProperties
import androidx.media3.datasource.HttpUtil
import com.google.common.net.HttpHeaders
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.isSuccessful
import com.lasthopesoftware.promises.extensions.toFuture
import com.lasthopesoftware.resources.io.PromisingReadableStream
import com.lasthopesoftware.resources.uri.toURL
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.ExecutionException
import kotlin.math.min

/**
 * An [HttpDataSource] that delegates to [HttpPromiseClient].
 *
 * Borrowed and modified with love from https://github.com/androidx/media/blob/release/libraries/datasource_okhttp/src/main/java/androidx/media3/datasource/okhttp/OkHttpDataSource.java.
 *
 * Note: HTTP request headers will be set using all parameters passed via (in order of decreasing
 * priority) the `dataSpec`, [.setRequestProperty] and the default parameters used to
 * construct the instance.
 */
@OptIn(UnstableApi::class)
class HttpPromiseClientDataSource private constructor(
	private val client: HttpPromiseClient,
	private val defaultRequestProperties: RequestProperties?
) : BaseDataSource( /* isNetwork= */true), HttpDataSource {
	/** [androidx.media3.datasource.DataSource.Factory] for [HttpPromiseClientDataSource] instances.  */
	@OptIn(UnstableApi::class)
	class Factory(private val client: HttpPromiseClient) : HttpDataSource.Factory {
		private val defaultRequestProperties: RequestProperties = RequestProperties()

		@CanIgnoreReturnValue
		@UnstableApi
		override fun setDefaultRequestProperties(defaultRequestProperties: MutableMap<String, String>): Factory {
			this.defaultRequestProperties.clearAndSet(defaultRequestProperties)
			return this
		}

		@UnstableApi
		override fun createDataSource(): HttpPromiseClientDataSource {
			val dataSource = HttpPromiseClientDataSource(client, defaultRequestProperties)
			return dataSource
		}
	}

	private val requestProperties = RequestProperties()
	private var dataSpec: DataSpec? = null
	private var response: HttpResponse? = null
	private var responseByteStream: PromisingReadableStream? = null
	private var connectionEstablished = false
	private var bytesToRead: Long = 0
	private var bytesRead: Long = 0

	@UnstableApi
	override fun getUri(): Uri? = dataSpec?.uri

	@UnstableApi
	override fun getResponseCode(): Int = response?.code ?: -1

	@UnstableApi
	override fun getResponseHeaders(): Map<String, List<String>> = response?.headers ?: emptyMap()

	@UnstableApi
	override fun setRequestProperty(name: String, value: String) {
		requestProperties.set(name, value)
	}

	@UnstableApi
	override fun clearRequestProperty(name: String) {
		requestProperties.remove(name)
	}

	@UnstableApi
	override fun clearAllRequestProperties() {
		requestProperties.clear()
	}

	@UnstableApi
	@Throws(HttpDataSourceException::class)
	override fun open(dataSpec: DataSpec): Long {
		this.dataSpec = dataSpec
		bytesRead = 0
		bytesToRead = 0
		transferInitializing(dataSpec)

		val promisedResponse = client
			.promiseResponse(
				method = dataSpec.httpMethodString,
				headers = getRequestHeaders(dataSpec),
				url = dataSpec.uri.toURL()
			)
		val response = try {
			promisedResponse
				.toFuture()
				.get()!!
		} catch (_: InterruptedException) {
			promisedResponse.cancel()
			throw HttpDataSourceException.createForIOException(
				InterruptedIOException(), dataSpec, HttpDataSourceException.TYPE_OPEN
			)
		} catch (ee: ExecutionException) {
			throw HttpDataSourceException.createForIOException(
				IOException(ee), dataSpec, HttpDataSourceException.TYPE_OPEN
			)
		} catch (e: IOException) {
			throw HttpDataSourceException.createForIOException(
				e, dataSpec, HttpDataSourceException.TYPE_OPEN
			)
		}
		responseByteStream = response.body

		val responseCode = response.code

		// Check for a valid response code.
		if (!response.isSuccessful) {
			if (responseCode == 416) {
				val documentSize =
					HttpUtil.getDocumentSize(responseHeaders[HttpHeaders.CONTENT_RANGE]?.first())
				if (dataSpec.position == documentSize) {
					connectionEstablished = true
					transferStarted(dataSpec)
					return if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else 0
				}
			}
			val errorResponseBody = responseByteStream?.let {
				try {
					it.promiseReadAllBytes().toFuture().get()
				} catch (_: IOException) {
					Util.EMPTY_BYTE_ARRAY
				}
			} ?: Util.EMPTY_BYTE_ARRAY
			val headers = response.headers
			closeConnectionQuietly()
			val cause: IOException? =
				if (responseCode == 416)
					DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
				else
					null
			throw HttpDataSource.InvalidResponseCodeException(
				responseCode, response.message, cause, headers, dataSpec, errorResponseBody
			)
		}

		// If we requested a range starting from a non-zero position and received a 200 rather than a
		// 206, then the server does not support partial requests. We'll need to manually skip to the
		// requested position.
		val bytesToSkip = if (responseCode == 200 && dataSpec.position != 0L) dataSpec.position else 0

		// Determine the length of the data to be read, after skipping.
		if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
			bytesToRead = dataSpec.length
		} else {
			val contentLength = response.contentLength
			bytesToRead = if (contentLength != -1L) (contentLength - bytesToSkip) else C.LENGTH_UNSET.toLong()
		}

		connectionEstablished = true
		transferStarted(dataSpec)

		try {
			skipFully(bytesToSkip, dataSpec)
		} catch (e: HttpDataSourceException) {
			closeConnectionQuietly()
			throw e
		}

		return bytesToRead
	}

	@UnstableApi
	@Throws(HttpDataSourceException::class)
	override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
		try {
			return readInternal(buffer, offset, length)
		} catch (e: IOException) {
			throw HttpDataSourceException.createForIOException(
				e, Util.castNonNull<DataSpec>(dataSpec), HttpDataSourceException.TYPE_READ
			)
		}
	}

	@UnstableApi
	override fun close() {
		if (connectionEstablished) {
			connectionEstablished = false
			transferEnded()
			closeConnectionQuietly()
		}
		response = null
		dataSpec = null
	}

	private fun getRequestHeaders(dataSpec: DataSpec): Map<String, String> {
		val headers = HashMap<String, String>()
		defaultRequestProperties?.snapshot?.let(headers::putAll)

		headers.putAll(requestProperties.getSnapshot())
		headers.putAll(dataSpec.httpRequestHeaders)

		val position = dataSpec.position
		val length = dataSpec.length
		val rangeHeader = HttpUtil.buildRangeRequestHeader(position, length)
		if (rangeHeader != null) {
			headers[HttpHeaders.RANGE] = rangeHeader
		}
		if (!dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) {
			headers[HttpHeaders.ACCEPT_ENCODING] = "identity"
		}
		return headers
	}

	/**
	 * Attempts to skip the specified number of bytes in full.
	 *
	 * @param bytesToSkip The number of bytes to skip.
	 * @param dataSpec The [DataSpec].
	 * @throws HttpDataSourceException If the thread is interrupted during the operation, or an error
	 * occurs while reading from the source, or if the data ended before skipping the specified
	 * number of bytes.
	 */
	@OptIn(UnstableApi::class)
	@Throws(HttpDataSourceException::class)
	private fun skipFully(bytesToSkip: Long, dataSpec: DataSpec) {
		var bytesToSkip = bytesToSkip
		if (bytesToSkip == 0L) {
			return
		}
		val skipBuffer = ByteArray(4096)
		try {
			while (bytesToSkip > 0) {
				val readLength = min(bytesToSkip, skipBuffer.size.toLong()).toInt()
				val read = try {
					val promisedRead = responseByteStream?.promiseRead(skipBuffer, 0, readLength)
					if (Thread.currentThread().isInterrupted) {
						promisedRead?.cancel()
						throw InterruptedIOException()
					}
					promisedRead?.toFuture()?.get()
				} catch (ee: ExecutionException) {
					throw ee.cause ?: ee
				}
				if (read == null || read == -1) {
					throw HttpDataSourceException(
						dataSpec,
						PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
						HttpDataSourceException.TYPE_OPEN
					)
				}
				bytesToSkip -= read.toLong()
				bytesTransferred(read)
			}
			return
		} catch (e: IOException) {
			if (e is HttpDataSourceException) {
				throw e
			} else {
				throw HttpDataSourceException(
					dataSpec,
					PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
					HttpDataSourceException.TYPE_OPEN
				)
			}
		}
	}

	/**
	 * Reads up to `length` bytes of data and stores them into `buffer`, starting at index
	 * `offset`.
	 *
	 *
	 * This method blocks until at least one byte of data can be read, the end of the opened range
	 * is detected, or an exception is thrown.
	 *
	 * @param buffer The buffer into which the read data should be stored.
	 * @param offset The start offset into `buffer` at which data should be written.
	 * @param readLength The maximum number of bytes to read.
	 * @return The number of bytes read, or [C.RESULT_END_OF_INPUT] if the end of the opened
	 * range is reached.
	 * @throws IOException If an error occurs reading from the source.
	 */
	@OptIn(UnstableApi::class)
	@Throws(IOException::class)
	private fun readInternal(buffer: ByteArray, offset: Int, readLength: Int): Int {
		if (readLength == 0) {
			return 0
		}

		var readLength = readLength
		if (bytesToRead != C.LENGTH_UNSET.toLong()) {
			val bytesRemaining = bytesToRead - bytesRead
			if (bytesRemaining == 0L) {
				return C.RESULT_END_OF_INPUT
			}
			readLength = min(readLength.toLong(), bytesRemaining).toInt()
		}

		val read = try {
			responseByteStream?.promiseRead(buffer, offset, readLength)?.toFuture()?.get()
		} catch (ee: ExecutionException) {
			throw ee.cause ?: ee
		}

		if (read == -1 || read == null) {
			return C.RESULT_END_OF_INPUT
		}

		bytesRead += read.toLong()
		bytesTransferred(read)
		return read
	}

	/** Closes the current connection quietly, if there is one.  */
	private fun closeConnectionQuietly() {
		response?.promiseClose()
		responseByteStream = null
	}
}
