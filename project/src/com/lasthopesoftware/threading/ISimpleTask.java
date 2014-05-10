package com.lasthopesoftware.threading;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;


public interface ISimpleTask<TParams, TProgress, TResult> {

	TResult getResult() throws ExecutionException, InterruptedException;
	
	LinkedList<Exception> getExceptions();
	
	SimpleTaskState getState();
	
	void addOnStartListener(OnStartListener<TParams, TProgress, TResult> listener);
	void addOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener);
	void addOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener);
	void addOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener);
	
	void setOnExecuteListener(OnExecuteListener<TParams, TProgress, TResult> listener);
	
	void removeOnStartListener(OnStartListener<TParams, TProgress, TResult> listener);
	void removeOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener);
	void removeOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener);
	void removeOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener);
	
	/* Events */
	public interface OnStartListener<TParams, TProgress, TResult> {
		void onStart(ISimpleTask<TParams, TProgress, TResult> owner);
	}
	
	public interface OnExecuteListener<TParams, TProgress, TResult> {
		TResult onExecute(ISimpleTask<TParams, TProgress, TResult> owner, TParams... params) throws Exception;
	}
	
	public interface OnProgressListener<TParams, TProgress, TResult> {
		void onReportProgress(ISimpleTask<TParams, TProgress, TResult> owner, TProgress...progresses);
	}
	
	public interface OnCompleteListener<TParams, TProgress, TResult> {
		void onComplete(ISimpleTask<TParams, TProgress, TResult> owner, TResult result);
	}
	
	public interface OnErrorListener<TParams, TProgress, TResult> {
		boolean onError(ISimpleTask<TParams, TProgress, TResult> owner, Exception innerException);
	}
}
