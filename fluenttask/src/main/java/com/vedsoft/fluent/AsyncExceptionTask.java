package com.vedsoft.fluent;

import android.os.AsyncTask;

public abstract class AsyncExceptionTask<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> {

	private Exception exception;

	public Exception getException() {
		return exception;
	}

	protected void setException(Exception exception) {
		this.exception = exception;
	}
	
	@Override
	protected final void onPostExecute(TResult result) {
		onPostExecute(result, exception);
	}
	
	@Override
	protected final void onCancelled(TResult result) {
		onCancelled(result, exception);
	}
	
	protected void onPostExecute(TResult result, Exception exception) {
	}
	
	protected void onCancelled(TResult result, Exception exception) {
	}
}
