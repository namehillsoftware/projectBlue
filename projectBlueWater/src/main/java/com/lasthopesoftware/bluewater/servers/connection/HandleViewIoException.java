package com.lasthopesoftware.bluewater.servers.connection;

import android.content.Context;

import com.lasthopesoftware.threading.ISimpleTask;

import java.io.IOException;

public class HandleViewIoException<TParams, TProgress, TResult> implements ISimpleTask.OnErrorListener<TParams, TProgress, TResult> {
	
	private final Context mContext;
	private final Runnable mOnConnectionRegainedListener;
	
	@Override
	public boolean onError(ISimpleTask<TParams, TProgress, TResult> owner, boolean isHandled, Exception innerException) {
		if (isHandled || !(innerException instanceof IOException)) return false;
		
		WaitForConnectionActivity.beginWaiting(mContext, mOnConnectionRegainedListener);

		return true;
	}
	
	public HandleViewIoException(final Context context, final Runnable onConnectionRegainedListener) {
		mContext = context;
		mOnConnectionRegainedListener = onConnectionRegainedListener;
	}
}
