package com.lasthopesoftware.threading;

import android.os.AsyncTask;

public abstract class AsyncExceptionTask<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> {

	private Exception mException;
	
	@Override
	protected final void onPostExecute(TResult result) {
		onPostExecute(result, mException);
	}
	
	protected void onPostExecute(TResult result, Exception exception) {
	}
}
