package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionActivity;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

public class HandleViewIoException implements ImmediateResponse<Throwable, Void> {
	
	private final Context context;
	private final Runnable onConnectionRegainedListener;

	public HandleViewIoException(final Context context, final Runnable onConnectionRegainedListener) {
		this.context = context;
		this.onConnectionRegainedListener = onConnectionRegainedListener;
	}

	@Override
	public Void respond(Throwable e) throws Throwable {
		if (ConnectionLostExceptionFilter.isConnectionLostException(e))
			WaitForConnectionActivity.beginWaiting(context, onConnectionRegainedListener);

		throw e;
	}
}
