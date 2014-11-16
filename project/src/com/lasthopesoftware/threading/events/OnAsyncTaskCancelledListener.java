package com.lasthopesoftware.threading.events;

import android.os.AsyncTask;

public interface OnAsyncTaskCancelledListener<TParams, TProgress, TResult> {
	void onAsyncTaskCancelled(AsyncTask<TParams, TProgress, TResult> task, TResult result);
}
