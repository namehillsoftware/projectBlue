package com.lasthopesoftware.bluewater.client.connection.trust

import android.content.ContentResolver
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.SelectDocumentUris
import com.lasthopesoftware.resources.uri.toUri
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.net.URI
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


class UserSslCertificateProvider(
	private val selectDocumentUris: SelectDocumentUris,
	private val contentResolver: ContentResolver
) : ProvideUserSslCertificates, PromisedResponse<URI?, ByteArray> {

	companion object {
		private val emptyByteArray by lazy { ByteArray(0) }
		private val x509CertificateFactory by lazy { CertificateFactory.getInstance("X.509") }
	}

	override fun promiseUserSslCertificateFingerprint(): Promise<ByteArray> =
		selectDocumentUris
			.promiseSelectedDocumentUri("application/x-x509*")
			.eventually(this)

	override fun promiseResponse(documentUri: URI?): Promise<ByteArray> =
		documentUri
			?.toUri()
			?.let { uri ->
				QueuedPromise(MessageWriter {
					contentResolver.openInputStream(uri)
						?.use { stream ->
							x509CertificateFactory.generateCertificate(stream) as? X509Certificate
						}
						?.let(::getThumbPrint)
						?: emptyByteArray
				}, ThreadPools.io)
			}
			.keepPromise(emptyByteArray)
}
