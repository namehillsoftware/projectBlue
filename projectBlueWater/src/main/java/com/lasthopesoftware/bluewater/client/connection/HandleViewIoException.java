package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionActivity;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

public class HandleViewIoException implements ImmediateResponse<Throwable, Void> {
	
	private final Context context;
	private final Runnable onConnectionRegainedListener;

	public HandleViewIoException(final Context context, final Runnable onConnectionRegainedListener) {
		this.context = context;
		this.onConnectionRegainedListener = onConnectionRegainedListener;
	}

	@Override
	public Void respond(Throwable e) throws Throwable {
		if (isConnectionLostException(e))
			WaitForConnectionActivity.beginWaiting(context, onConnectionRegainedListener);

		throw e;
	}

	private boolean isConnectionLostException(Throwable error) {
		return error instanceof IOException && isConnectionLostException((IOException)error);
	}

	private boolean isConnectionLostException(IOException ioException) {
		return ioException instanceof SocketTimeoutException
			|| ioException instanceof EOFException
			|| ioException instanceof ConnectException;
	}
}
