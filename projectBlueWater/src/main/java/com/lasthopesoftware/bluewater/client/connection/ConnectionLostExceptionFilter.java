package com.lasthopesoftware.bluewater.client.connection;

import javax.net.ssl.SSLException;
import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ConnectionLostExceptionFilter {
	public static boolean isConnectionLostException(Throwable error) {
		return error instanceof IOException && isConnectionLostException((IOException)error);
	}

	private static boolean isConnectionLostException(IOException ioException) {
		return ioException instanceof SocketTimeoutException
			|| ioException instanceof EOFException
			|| ioException instanceof ConnectException
			|| ioException instanceof UnknownHostException
			|| ioException instanceof SSLException;
	}
}
