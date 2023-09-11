package com.lasthopesoftware.bluewater.client.connection.trust

import java.security.MessageDigest
import java.security.cert.X509Certificate

// Thank you: http://stackoverflow.com/questions/1270703/how-to-retrieve-compute-an-x509-certificates-thumbprint-in-java
fun getThumbPrint(cert: X509Certificate): ByteArray {
	val md = MessageDigest.getInstance("SHA-1")
	md.update(cert.encoded)
	return md.digest()
}
