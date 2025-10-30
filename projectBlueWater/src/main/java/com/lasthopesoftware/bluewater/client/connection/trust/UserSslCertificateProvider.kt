package com.lasthopesoftware.bluewater.client.connection.trust

import android.content.ContentResolver
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.SelectDocumentUris
import com.lasthopesoftware.resources.uri.toUri
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.net.URI
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


class UserSslCertificateProvider(
	private val selectDocumentUris: SelectDocumentUris,
	private val contentResolver: ContentResolver
) : ProvideUserSslCertificates, PromisedResponse<URI?, ByteArray> {

	companion object {
		private val x509CertificateFactory by lazy { CertificateFactory.getInstance("X.509") }
	}

	override fun promiseUserSslCertificateFingerprint(): Promise<ByteArray> =
		selectDocumentUris
			.promiseSelectedDocumentUri(
				"application/x-x509-ca-cert",
				"application/x-x509-user-cert",
				"application/x-pem-file",
			)
			.eventually(this)

	override fun promiseResponse(documentUri: URI?): Promise<ByteArray> =
		documentUri
			?.toUri()
			?.let { uri ->
				ThreadPools.io.preparePromise {
					contentResolver
						.openInputStream(uri)
						?.use { stream ->
							x509CertificateFactory.generateCertificate(stream) as? X509Certificate
						}
						?.let(::getThumbPrint)
						?: emptyByteArray
				}
			}
			.keepPromise { emptyByteArray }
}
