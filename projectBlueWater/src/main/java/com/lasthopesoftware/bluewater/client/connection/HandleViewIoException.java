package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;

import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import java.io.IOException;

public class HandleViewIoException implements
	CarelessOneParameterFunction<Throwable, Void> {
	
	private final Context context;
	private final Runnable onConnectionRegainedListener;

	public HandleViewIoException(final Context context, final Runnable onConnectionRegainedListener) {
		this.context = context;
		this.onConnectionRegainedListener = onConnectionRegainedListener;
	}

	@Override
	public Void resultFrom(Throwable e) {
		if (e instanceof IOException)
			WaitForConnectionActivity.beginWaiting(context, onConnectionRegainedListener);

		return null;
	}
}
