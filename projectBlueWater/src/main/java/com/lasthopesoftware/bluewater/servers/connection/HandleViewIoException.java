package com.lasthopesoftware.bluewater.servers.connection;

import android.content.Context;

import com.lasthopesoftware.threading.IFluentTask;

import java.io.IOException;

public class HandleViewIoException<TParams, TProgress, TResult> implements IFluentTask.OnErrorListener<TParams, TProgress, TResult> {
	
	private final Context mContext;
	private final Runnable mOnConnectionRegainedListener;
	
	@Override
	public boolean onError(IFluentTask<TParams, TProgress, TResult> owner, boolean isHandled, Exception innerException) {
		if (isHandled || !(innerException instanceof IOException)) return false;
		
		WaitForConnectionActivity.beginWaiting(mContext, mOnConnectionRegainedListener);

		return true;
	}
	
	public HandleViewIoException(final Context context, final Runnable onConnectionRegainedListener) {
		mContext = context;
		mOnConnectionRegainedListener = onConnectionRegainedListener;
	}
}
