package com.lasthopesoftware.threading.events;

import android.os.AsyncTask;

public interface OnAsyncTaskCompleteListener<TParams, TProgress, TResult> {
	void onAsyncTaskComplete(AsyncTask<TParams, TProgress, TResult> task, TResult result);
}
