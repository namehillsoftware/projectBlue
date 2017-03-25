package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;

import com.vedsoft.fluent.IFluentTask;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.callables.TwoParameterFunction;

import java.io.IOException;

public class HandleViewIoException<TParams, TProgress, TResult> implements
	TwoParameterFunction<IFluentTask<TParams, TProgress, TResult>, Exception, Boolean>,
	CarelessOneParameterFunction<Throwable, Void> {
	
	private final Context context;
	private final Runnable onConnectionRegainedListener;
	
	@Override
	public Boolean resultFrom(IFluentTask<TParams, TProgress, TResult> owner, Exception innerException) {
		if (!(innerException instanceof IOException)) return false;
		
		WaitForConnectionActivity.beginWaiting(context, onConnectionRegainedListener);

		return true;
	}
	
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
