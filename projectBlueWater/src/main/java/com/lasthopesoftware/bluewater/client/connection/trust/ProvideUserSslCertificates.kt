package com.lasthopesoftware.bluewater.client.connection.trust

import com.namehillsoftware.handoff.promises.Promise

interface ProvideUserSslCertificates {
	fun promiseUserSslCertificateFingerprint(): Promise<ByteArray>
}
