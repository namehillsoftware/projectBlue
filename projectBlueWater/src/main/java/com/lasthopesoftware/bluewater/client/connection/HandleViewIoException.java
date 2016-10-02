package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;

import com.vedsoft.fluent.FluentSpecifiedTask;
import com.vedsoft.futures.callables.TwoParameterCallable;

import java.io.IOException;

public class HandleViewIoException<TParams, TProgress, TResult> implements TwoParameterCallable<FluentSpecifiedTask<TParams, TProgress, TResult>, Exception, Boolean> {
	
	private final Context mContext;
	private final Runnable mOnConnectionRegainedListener;
	
	@Override
	public Boolean call(FluentSpecifiedTask<TParams, TProgress, TResult> owner, Exception innerException) {
		if (!(innerException instanceof IOException)) return false;
		
		WaitForConnectionActivity.beginWaiting(mContext, mOnConnectionRegainedListener);

		return true;
	}
	
	public HandleViewIoException(final Context context, final Runnable onConnectionRegainedListener) {
		mContext = context;
		mOnConnectionRegainedListener = onConnectionRegainedListener;
	}
}
