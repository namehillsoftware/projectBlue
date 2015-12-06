package com.lasthopesoftware.threading;

import android.os.AsyncTask;

public abstract class AsyncExceptionTask<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> {

	private Exception mException;
	
	public boolean hasError() {
		return mException != null;
	}
	
	protected void setException(Exception exception) {
		mException = exception;
	}
	
	@Override
	protected final void onPostExecute(TResult result) {
		onPostExecute(result, mException);
	}
	
	@Override
	protected final void onCancelled(TResult result) {
		onCancelled(result, mException);
	}
	
	protected void onPostExecute(TResult result, Exception exception) {
	}
	
	protected void onCancelled(TResult result, Exception exception) {
	}
}
