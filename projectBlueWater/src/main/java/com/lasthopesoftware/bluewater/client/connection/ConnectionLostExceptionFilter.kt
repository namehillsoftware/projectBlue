package com.lasthopesoftware.bluewater.client.connection

import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object ConnectionLostExceptionFilter {
    fun isConnectionLostException(error: Throwable?): Boolean =
		error is IOException && isConnectionLostException(error)

    private fun isConnectionLostException(ioException: IOException): Boolean {
        return (ioException is SocketTimeoutException
                || ioException is EOFException
                || ioException is ConnectException
                || ioException is UnknownHostException
                || ioException is SSLException)
    }
}
