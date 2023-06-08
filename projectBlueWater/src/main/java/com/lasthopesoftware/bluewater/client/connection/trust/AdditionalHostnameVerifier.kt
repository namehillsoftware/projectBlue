package com.lasthopesoftware.bluewater.client.connection.trust

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

class AdditionalHostnameVerifier(
    private val additionalHostname: String,
    private val fallbackHostnameVerifier: HostnameVerifier?
) : HostnameVerifier {
    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        return hostname != null && hostname.equals(
            additionalHostname,
            ignoreCase = true
        ) || fallbackHostnameVerifier != null && fallbackHostnameVerifier.verify(
            hostname,
            session
        )
    }
}
