package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;

import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.io.IOException;

public class HandleViewIoException implements ImmediateResponse<Throwable, Void> {
	
	private final Context context;
	private final Runnable onConnectionRegainedListener;

	public HandleViewIoException(final Context context, final Runnable onConnectionRegainedListener) {
		this.context = context;
		this.onConnectionRegainedListener = onConnectionRegainedListener;
	}

	@Override
	public Void respond(Throwable e) {
		if (e instanceof IOException)
			WaitForConnectionActivity.beginWaiting(context, onConnectionRegainedListener);

		return null;
	}
}
