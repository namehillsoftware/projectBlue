package com.lasthopesoftware.bluewater.servers.connection;

import android.content.Context;

import com.lasthopesoftware.callables.IThreeParameterCallable;
import com.lasthopesoftware.threading.IFluentTask;

import java.io.IOException;

public class HandleViewIoException<TParams, TProgress, TResult> implements IThreeParameterCallable<IFluentTask<TParams, TProgress, TResult>, Boolean, Exception, Boolean> {
	
	private final Context mContext;
	private final Runnable mOnConnectionRegainedListener;
	
	@Override
	public Boolean call(IFluentTask<TParams, TProgress, TResult> owner, Boolean isHandled, Exception innerException) {
		if (isHandled || !(innerException instanceof IOException)) return false;
		
		WaitForConnectionActivity.beginWaiting(mContext, mOnConnectionRegainedListener);

		return true;
	}
	
	public HandleViewIoException(final Context context, final Runnable onConnectionRegainedListener) {
		mContext = context;
		mOnConnectionRegainedListener = onConnectionRegainedListener;
	}
}
