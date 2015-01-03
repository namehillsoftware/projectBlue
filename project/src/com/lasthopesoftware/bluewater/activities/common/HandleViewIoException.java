package com.lasthopesoftware.bluewater.activities.common;

import java.io.IOException;

import android.content.Context;

import com.lasthopesoftware.bluewater.activities.WaitForConnection;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.threading.ISimpleTask;

@SuppressWarnings("rawtypes")
public class HandleViewIoException implements ISimpleTask.OnErrorListener, IDataTask.OnErrorListener {
	
	private final Context mContext;
	private final OnConnectionRegainedListener mOnConnectionRegainedListener;
	
	@Override
	public boolean onError(ISimpleTask owner, boolean isHandled, Exception innerException) {
		if (isHandled || !(innerException instanceof IOException)) return false;
		
		WaitForConnection.beginWaiting(mContext, mOnConnectionRegainedListener);

		return true;
	}
	
	public HandleViewIoException(final Context context, final OnConnectionRegainedListener onConnectionRegainedListener) {		
		mContext = context;
		mOnConnectionRegainedListener = onConnectionRegainedListener;
	}
}
