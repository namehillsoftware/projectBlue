package com.lasthopesoftware.threading;

import java.util.concurrent.ExecutionException;

public interface ISimpleTask<TParams, TProgress, TResult> {

	TResult get() throws ExecutionException, InterruptedException;
	
	Exception getException();
	
	SimpleTaskState getState();
	
	ISimpleTask<TParams, TProgress, TResult> addOnStartListener(OnStartListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> addOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> addOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> addOnCancelListener(OnCancelListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> addOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener);
	
	ISimpleTask<TParams, TProgress, TResult> removeOnStartListener(OnStartListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> removeOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> removeOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> removeOnCancelListener(OnCancelListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> removeOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener);
	
	ISimpleTask<TParams, TProgress, TResult> cancel(boolean interrupt);
	boolean isCancelled();
	
	/* Events */
	public interface OnStartListener<TParams, TProgress, TResult> {
		void onStart(ISimpleTask<TParams, TProgress, TResult> owner);
	}
	
	public interface OnExecuteListener<TParams, TProgress, TResult> {
		@SuppressWarnings("unchecked")
		TResult onExecute(ISimpleTask<TParams, TProgress, TResult> owner, TParams... params) throws Exception;
	}
	
	public interface OnProgressListener<TParams, TProgress, TResult> {
		@SuppressWarnings("unchecked")
		void onReportProgress(ISimpleTask<TParams, TProgress, TResult> owner, TProgress...progresses);
	}
	
	public interface OnCompleteListener<TParams, TProgress, TResult> {
		void onComplete(ISimpleTask<TParams, TProgress, TResult> owner, TResult result);
	}
	
	public interface OnCancelListener<TParams, TProgress, TResult> {
		void onCancel(ISimpleTask<TParams, TProgress, TResult> owner, TResult result);
	}
	
	public interface OnErrorListener<TParams, TProgress, TResult> {
		boolean onError(ISimpleTask<TParams, TProgress, TResult> owner, boolean isHandled, Exception innerException);
	}
}
