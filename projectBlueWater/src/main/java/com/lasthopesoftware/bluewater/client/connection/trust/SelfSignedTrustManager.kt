/*
 * Copyright 2010-2013 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lasthopesoftware.bluewater.client.connection.trust

import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Arrays
import javax.net.ssl.X509TrustManager

class SelfSignedTrustManager(
    private val certificateFingerprint: ByteArray?,
    private val fallbackTrustManager: X509TrustManager
) : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        fallbackTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String?) {
        if (certificateFingerprint == null) {
            throw CertificateException("Requires a non-null certificateFingerprint key in SHA-1 format to match.")
        }

        // We have a certKey defined. We should now examine the one we got from the server.
        // They match? All is good. They don't, throw an exception.
        try {
            // Assume self-signed root is okay
            if (Arrays.equals(certificateFingerprint, getThumbPrint(chain[0]))) return
        } catch (e: NoSuchAlgorithmException) {
            throw CertificateException(
                "Unable to check self-signed certificate, unknown algorithm. ",
                e
            )
        }
        fallbackTrustManager.checkServerTrusted(chain, authType)
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return Companion.acceptedIssuers
    }

    companion object {
        private val acceptedIssuers = arrayOf<X509Certificate>()
    }
}
