package com.lasthopesoftware.threading.events;

import android.os.AsyncTask;

public interface OnAsyncTaskExceptionListener<TParams, TProgress, TResult> {
	void onAsyncTaskException(AsyncTask<TParams, TProgress, TResult> task, TResult result, Exception exception);
}
