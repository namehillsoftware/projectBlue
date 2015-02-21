package com.lasthopesoftware.bluewater.servers.connection;

import java.io.IOException;

import android.content.Context;

import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.threading.IDataTask;
import com.lasthopesoftware.threading.ISimpleTask;

@SuppressWarnings("rawtypes")
public class HandleViewIoException implements ISimpleTask.OnErrorListener, IDataTask.OnErrorListener {
	
	private final Context mContext;
	private final OnConnectionRegainedListener mOnConnectionRegainedListener;
	
	@Override
	public boolean onError(ISimpleTask owner, boolean isHandled, Exception innerException) {
		if (isHandled || !(innerException instanceof IOException)) return false;
		
		WaitForConnectionActivity.beginWaiting(mContext, mOnConnectionRegainedListener);

		return true;
	}
	
	public HandleViewIoException(final Context context, final OnConnectionRegainedListener onConnectionRegainedListener) {		
		mContext = context;
		mOnConnectionRegainedListener = onConnectionRegainedListener;
	}
}
