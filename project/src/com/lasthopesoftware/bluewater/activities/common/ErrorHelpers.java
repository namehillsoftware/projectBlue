package com.lasthopesoftware.bluewater.activities.common;

import java.io.IOException;
import java.util.LinkedList;

import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.activities.WaitForConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.SimpleTaskState;

public class ErrorHelpers {
	@SuppressWarnings("rawtypes")
	public static final OnErrorListener OnSimpleIoExceptionErrors = new OnErrorListener() {
		
		@Override
		public boolean onError(ISimpleTask owner, Exception innerException) {
			return !(innerException instanceof IOException);
		}
	};
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final boolean HandleViewIoException(final Context context, ISimpleTask task, final OnConnectionRegainedListener onConnectionRegainedListener) {
		if (task.getState() != SimpleTaskState.ERROR) return false;
		
		for (Exception exception : (LinkedList<Exception>)task.getExceptions()) {
			if (exception instanceof IOException) {
				
				final PollConnection pollConnectionInstance = PollConnection.Instance.get(context);
				pollConnectionInstance.addOnConnectionRegainedListener(onConnectionRegainedListener);
				pollConnectionInstance.startPolling();
				context.startActivity(new Intent(context, WaitForConnection.class));

				return true;
			}
		}
		
		return false;
	}
}
