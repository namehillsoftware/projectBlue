package com.lasthopesoftware.threading;

import java.io.InputStream;

public interface IDataTask<TResult> extends ISimpleTask<String, Void, TResult> {

	public interface OnConnectListener<TResult> {
		TResult onConnect(InputStream is);
	}
	
	/* Events */
	public interface OnStartListener<TResult> extends ISimpleTask.OnStartListener<String, Void, TResult> {
	}
		
	public interface OnProgressListener<TResult> extends ISimpleTask.OnProgressListener<String, Void, TResult> {
	}
	
	public interface OnCompleteListener<TResult> extends ISimpleTask.OnCompleteListener<String, Void, TResult> {
	}
		
	public interface OnErrorListener<TResult> extends ISimpleTask.OnErrorListener<String, Void, TResult> {
	}
}
